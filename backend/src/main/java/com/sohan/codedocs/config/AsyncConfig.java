package com.sohan.codedocs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@EnableResilientMethods
public class AsyncConfig {

    public static final String INGESTION_EXECUTOR = "ingestionExecutor";
    public static final String CHAT_STREAM_EXECUTOR = "chatStreamExecutor";

    @Bean(name = INGESTION_EXECUTOR)
    public Executor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("ingest-");
        executor.setRejectedExecutionHandler(loggingAbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * One thread per open chat stream for the duration of that stream (the
     * Gemini HTTP read is blocking), so this needs more headroom than
     * ingestion — a handful of people asking questions concurrently is the
     * normal case, not the exception.
     */
    @Bean(name = CHAT_STREAM_EXECUTOR)
    public Executor chatStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("chat-stream-");
        executor.setRejectedExecutionHandler(loggingAbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    private RejectedExecutionHandler loggingAbortPolicy() {
        ThreadPoolExecutor.AbortPolicy delegate = new ThreadPoolExecutor.AbortPolicy();
        return (runnable, executor) -> {
            log.warn("Ingestion queue saturated (active={}, queued={}); rejecting task",
                    executor.getActiveCount(), executor.getQueue().size());
            delegate.rejectedExecution(runnable, executor);
        };
    }
}