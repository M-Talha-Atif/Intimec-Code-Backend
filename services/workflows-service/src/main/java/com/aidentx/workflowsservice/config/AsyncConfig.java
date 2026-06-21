package com.aidentx.workflowsservice.config;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Configuration for asynchronous workflow execution.
 * 
 * This configures the thread pool used for background workflow execution.
 * Key features:
 * - Configurable pool sizes (via application.properties)
 * - Graceful shutdown (waits for tasks to complete)
 * - CallerRunsPolicy for graceful degradation under load
 * - Named threads for easier debugging
 * 
 * Thread pool tuning guide:
 * - Core size: Number of threads always kept alive (10 default)
 * - Max size: Maximum threads under load (20 default)
 * - Queue capacity: How many executions can wait (500 default)
 * 
 * Scaling recommendations:
 * - Small deployment (1-10 workflows/min): core=5, max=10, queue=100
 * - Medium deployment (10-100 workflows/min): core=10, max=20, queue=500
 * - Large deployment (100+ workflows/min): core=20, max=50, queue=1000
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    private final IUnifiedLogger logger;
    
    @Value("${workflow.executor.thread-pool.core-size:10}")
    private int corePoolSize;
    
    public AsyncConfig(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(AsyncConfig.class, LOG_CATEGORY_WORKFLOW_CONFIG);
    }
    
    @Value("${workflow.executor.thread-pool.max-size:20}")
    private int maxPoolSize;
    
    @Value("${workflow.executor.thread-pool.queue-capacity:500}")
    private int queueCapacity;
    
    @Value("${workflow.executor.thread-pool.await-termination-seconds:60}")
    private int awaitTerminationSeconds;
    
    /**
     * Thread pool for workflow execution.
     * 
     * Configuration:
     * - Core pool size: Minimum threads always alive
     * - Max pool size: Maximum concurrent executions
     * - Queue capacity: How many can wait before rejection
     * - Rejected policy: CallerRunsPolicy (runs in caller thread if pool full)
     * - Shutdown: Waits up to 60s for tasks to complete
     * 
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "workflowExecutorThreadPool")
    public ThreadPoolTaskExecutor workflowExecutorThreadPool() {
        logger.info()
            .message(LOG_INITIALIZING_THREAD_POOL)
            .category(LOG_CATEGORY_WORKFLOW_CONFIG)
            .detail("corePoolSize", corePoolSize)
            .detail("maxPoolSize", maxPoolSize)
            .detail("queueCapacity", queueCapacity)
            .log();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Pool size configuration
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        
        // Thread naming for easier debugging
        executor.setThreadNamePrefix("workflow-exec-");
        
        // Rejected execution policy: run in caller thread (graceful degradation)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Graceful shutdown configuration
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        
        // Allow core threads to timeout (better resource usage)
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        
        // Initialize the executor
        executor.initialize();
        
        logger.info().message(LOG_THREAD_POOL_INITIALIZED).category(LOG_CATEGORY_WORKFLOW_CONFIG).log();
        
        return executor;
    }
}
