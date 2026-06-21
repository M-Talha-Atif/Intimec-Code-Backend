package com.aidentx.iam.shadow_management.sync;

import com.aidentx.dal.protos.shadow.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.FlushMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise-grade chunk transaction processor for IGA synchronization.
 *
 * <p>Instead of processing each record in its own auto-commit transaction
 * (300K+ individual SQL statements for 100K accounts), this processor
 * groups records into chunks and wraps each chunk in a single transaction.
 *
 * <p>With Hibernate batch inserts (batch_size=50), a chunk of 50 records
 * produces ~3 batch SQL statements (1 per entity table: shadows, identities,
 * employees) instead of ~150 individual INSERTs.
 *
 * <p><b>Error handling strategy:</b> If a chunk transaction fails, it's retried
 * record-by-record with individual transactions to isolate the failing record
 * while preserving the performance of the happy path.
 *
 * <p>This is a separate Spring bean to ensure @Transactional works through
 * Spring's proxy mechanism (self-invocation within the same class bypasses
 * the transactional proxy).
 */
@Service
public class SyncChunkProcessor {

    /** Chunk size aligned with Hibernate batch_size for optimal batching */
    static final int CHUNK_SIZE = 50;

    private final EntityManager entityManager;
    private final IUnifiedLogger logger;

    public SyncChunkProcessor(EntityManager entityManager, UnifiedLoggerFactory loggerFactory) {
        this.entityManager = entityManager;
        this.logger = loggerFactory.getLogger(SyncChunkProcessor.class);
    }

    /**
     * Processes a chunk of records within a single transaction.
     *
     * <p>All entities created/updated within this chunk are batched by Hibernate
     * at transaction commit. The EntityManager is flushed + cleared after commit
     * to release managed entities from the persistence context (bounded memory).
     *
     * @param chunk          The records to process
     * @param grpcService    The sync service to delegate per-record processing to
     * @return List of results (one per record, success or error)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ChunkItemResult> processChunk(
            List<BatchItemInput> chunk,
            SyncPipelineDelegate grpcService) {

        // ---------------------------------------------------------------
        // FlushMode.COMMIT is critical for Hibernate batch INSERT to work.
        //
        // Default FlushMode.AUTO flushes the persistence context before every
        // SELECT query if the queried table has pending changes. This means
        // record N's shadow lookup auto-flushes record N-1's shadow INSERT,
        // defeating batch INSERTs entirely (back to 1-by-1 SQL).
        //
        // With COMMIT mode, dirty entities accumulate in the persistence
        // context and are only flushed at our explicit flush() call or at
        // transaction commit. Hibernate then groups them into batch INSERTs
        // (50 shadows → 1 batch INSERT statement).
        //
        // This is safe because each record processes a UNIQUE resourceUid —
        // record 2 never queries for record 1's shadow. Correlation runs
        // against pre-existing identities, not those created within this chunk.
        // ---------------------------------------------------------------
        Session session = entityManager.unwrap(Session.class);
        FlushMode originalFlushMode = session.getHibernateFlushMode();
        session.setHibernateFlushMode(FlushMode.COMMIT);

        List<ChunkItemResult> results = new ArrayList<>(chunk.size());

        try {
            for (BatchItemInput item : chunk) {
                try {
                    CapturingObserver<ProcessImportedObjectResponse> captor = new CapturingObserver<>();
                    grpcService.processImportedObjectInternal(item.request(), captor);

                    ProcessImportedObjectResponse result = captor.getValue();
                    if (result != null) {
                        results.add(new ChunkItemResult(
                                item.resourceUid(),
                                result.toBuilder().setResourceUid(item.resourceUid()).build(),
                                true, null));
                    } else if (captor.getError() != null) {
                        results.add(ChunkItemResult.error(item.resourceUid(),
                                "Processing failed: " + captor.getError().getMessage()));
                    }
                } catch (Exception e) {
                    throw new ChunkProcessingException(
                            "Chunk failed at record " + item.resourceUid(), e, results, chunk);
                }
            }

            // Flush all accumulated entities in one go.
            // With order_inserts=true + batch_size=50, Hibernate groups INSERTs
            // by entity type and batches them:
            //   50 Shadow INSERTs    → 1 batch statement
            //   50 Identity INSERTs  → 1 batch statement
            //   50 Employee INSERTs  → 1 batch statement
            // = ~3 SQL round-trips instead of ~150
            entityManager.flush();
            entityManager.clear();

            return results;
        } finally {
            // Always restore original flush mode, even on failure
            session.setHibernateFlushMode(originalFlushMode);
        }
    }

    /**
     * Processes a single record in its own transaction.
     * Used as fallback when a chunk fails, to isolate the failing record.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChunkItemResult processSingleRecord(
            BatchItemInput item,
            SyncPipelineDelegate grpcService) {
        try {
            CapturingObserver<ProcessImportedObjectResponse> captor = new CapturingObserver<>();
            grpcService.processImportedObjectInternal(item.request(), captor);

            ProcessImportedObjectResponse result = captor.getValue();
            if (result != null) {
                return new ChunkItemResult(
                        item.resourceUid(),
                        result.toBuilder().setResourceUid(item.resourceUid()).build(),
                        true, null);
            } else if (captor.getError() != null) {
                return ChunkItemResult.error(item.resourceUid(),
                        "Processing failed: " + captor.getError().getMessage());
            }
            return ChunkItemResult.error(item.resourceUid(), "No result produced");
        } catch (Exception e) {
            logger.error(e).message("Single-record processing failed")
                    .detail("resourceUid", item.resourceUid())
                    .log();
            return ChunkItemResult.error(item.resourceUid(),
                    "Processing failed: " + e.getMessage());
        }
    }

    // ===================================================================
    //  Data Transfer Objects
    // ===================================================================

    public record BatchItemInput(
            String resourceUid,
            ProcessImportedObjectRequest request
    ) {}

    public record ChunkItemResult(
            String resourceUid,
            ProcessImportedObjectResponse response,
            boolean success,
            String errorMessage
    ) {
        static ChunkItemResult error(String resourceUid, String message) {
            return new ChunkItemResult(resourceUid,
                    ProcessImportedObjectResponse.newBuilder()
                            .setSuccess(false)
                            .setResourceUid(resourceUid)
                            .setMessage(message)
                            .build(),
                    false, message);
        }
    }

    /**
     * Delegate interface for the sync pipeline.
     * Decouples SyncChunkProcessor from ShadowSyncGrpcServiceImpl to avoid circular deps.
     */
    public interface SyncPipelineDelegate {
        void processImportedObjectInternal(
                ProcessImportedObjectRequest request,
                StreamObserver<ProcessImportedObjectResponse> responseObserver);
    }

    /**
     * Exception wrapping a chunk failure with partial results + remaining items.
     * The caller uses this to retry unprocessed items individually.
     */
    public static class ChunkProcessingException extends RuntimeException {
        private final List<ChunkItemResult> partialResults;
        private final List<BatchItemInput> fullChunk;

        public ChunkProcessingException(String message, Throwable cause,
                                        List<ChunkItemResult> partialResults,
                                        List<BatchItemInput> fullChunk) {
            super(message, cause);
            this.partialResults = partialResults;
            this.fullChunk = fullChunk;
        }

        public List<ChunkItemResult> getPartialResults() { return partialResults; }
        public List<BatchItemInput> getFullChunk() { return fullChunk; }
    }

    /**
     * A simple StreamObserver that captures the first value or error.
     */
    private static class CapturingObserver<T> implements StreamObserver<T> {
        private T value;
        private Throwable error;

        @Override public void onNext(T v) { this.value = v; }
        @Override public void onError(Throwable t) { this.error = t; }
        @Override public void onCompleted() {}

        public T getValue() { return value; }
        public Throwable getError() { return error; }
    }
}
