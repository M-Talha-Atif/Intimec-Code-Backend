package com.aidentx.authservice.grpc.service;

import com.aidentx.auth.grpc.SessionActivityServiceGrpc;
import com.aidentx.auth.grpc.UpdateLastActivityRequest;
import com.aidentx.auth.grpc.UpdateLastActivityResponse;
import com.aidentx.authservice.model.entity.UserSession;
import com.aidentx.authservice.repository.UserSessionRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * gRPC Service implementation for session activity tracking.
 * Updates the last activity timestamp for user sessions.
 */
@GrpcService
public class SessionActivityGrpcServiceImpl extends SessionActivityServiceGrpc.SessionActivityServiceImplBase {

    private final UserSessionRepository userSessionRepository;
    private final IUnifiedLogger logger;

    @Autowired
    public SessionActivityGrpcServiceImpl(
            UserSessionRepository userSessionRepository,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.userSessionRepository = userSessionRepository;
        this.logger = unifiedLoggerFactory.getLogger("SessionActivityGrpcService", "grpc-service");
    }

    @Override
    public void updateLastActivity(UpdateLastActivityRequest request, StreamObserver<UpdateLastActivityResponse> responseObserver) {
        try {
            String sessionId = request.getSessionId();
            
            if (sessionId == null || sessionId.trim().isEmpty()) {
                UpdateLastActivityResponse response = UpdateLastActivityResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Session ID is required")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Find session by sessionId
            Optional<UserSession> sessionOpt = userSessionRepository.findBySessionId(java.util.UUID.fromString(sessionId));
            
            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                session.setLastActivityAt(System.currentTimeMillis());
                userSessionRepository.save(session);
                
                logger.trace()
                        .message("Updated last activity for session")
                        .detail("sessionId", session.getSessionId().toString())
                        .detail("lastActivityAt", String.valueOf(session.getLastActivityAt()))
                        .log();
                
                UpdateLastActivityResponse response = UpdateLastActivityResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Last activity updated successfully")
                        .build();
                responseObserver.onNext(response);
            } else {
                logger.warn()
                        .message("Session not found for sessionId")
                        .detail("sessionId", sessionId)
                        .log();
                
                UpdateLastActivityResponse response = UpdateLastActivityResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Session not found")
                        .build();
                responseObserver.onNext(response);
            }
            
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error(e)
                    .message("Error updating last activity")
                    .log();
            
            UpdateLastActivityResponse response = UpdateLastActivityResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
