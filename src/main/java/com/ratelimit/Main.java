package com.ratelimit;

import com.ratelimit.service.impl.SlidingWindowLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    static void main() throws InterruptedException {
        // 1. Initialize our service
        SlidingWindowLog rateLimiter = new SlidingWindowLog();

        // 2. Configuration: 10 requests allowed every 5 seconds
        String clientId = "test-client-001";
        int limit = 10;
        int windowMs = 5000;

        // 3. Create a thread pool to simulate concurrent traffic
        // This is crucial to test if our Lua script handles race conditions!
        ExecutorService executor = Executors.newFixedThreadPool(5);

        System.out.println("Starting Rate Limiter Simulator...");
        System.out.println("Config: " + limit + " requests per " + (windowMs / 1000) + "s");

        // 4. Fire 20 requests rapidly (twice the allowed limit)
        for (int i = 1; i <= 20; i++) {
            int requestId = i;
            executor.submit(() -> {
                boolean allowed = rateLimiter.isAllowed(clientId, limit, windowMs);
                String status = allowed ? "ALLOWED" : "BLOCKED";

                // Get the container ID (if running in Docker) or thread name
                String source = System.getenv("HOSTNAME") != null ?
                        System.getenv("HOSTNAME") : Thread.currentThread().getName();

                System.out.printf("[%s] Request #%d: %s%n", source, requestId, status);
            });

            // Small sleep to spread them out slightly
            Thread.sleep(100);
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        System.out.println("Simulation complete. Keeping process alive for Docker logs...");
        // Keep the container running so we can inspect it
        Thread.currentThread().join();
    }
}
