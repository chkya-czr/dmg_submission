package com.example.notify.dispatch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Bounded dispatch resources: a fixed-size {@link ExecutorService} (the "bounded worker pool") and
 * a {@link Semaphore} of the same size that {@code DispatchScheduler} uses to gate how many claims
 * it attempts per poll, so it never queues more claimed work than the pool can run concurrently.
 */
@Configuration
public class WorkerPoolConfig {

    @Bean
    public ExecutorService dispatchExecutor(@Value("${notify.dispatch.worker-pool-size}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize, new CustomizableThreadFactory("dispatch-worker-"));
    }

    @Bean
    public Semaphore dispatchSemaphore(@Value("${notify.dispatch.worker-pool-size}") int poolSize) {
        return new Semaphore(poolSize);
    }

    @Bean
    public BackoffCalculator backoffCalculator(@Value("${notify.retry.base-delay-seconds}") int baseDelaySeconds,
                                                @Value("${notify.retry.max-delay-seconds}") int maxDelaySeconds) {
        return new BackoffCalculator(baseDelaySeconds, maxDelaySeconds, new Random());
    }
}
