package com.example.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThumbnailQueueManager { // Tên giữ nguyên, chức năng vẫn là quản lý queue
    private static volatile ThumbnailQueueManager instance;
    private final ExecutorService executorService;

    private ThumbnailQueueManager() {
        int numberOfThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        executorService = Executors.newFixedThreadPool(numberOfThreads);
        System.out.println("INFO: ImageProcessingQueueManager initialized with " + numberOfThreads + " threads.");
    }

    public static ThumbnailQueueManager getInstance() {
        if (instance == null) {
            synchronized (ThumbnailQueueManager.class) {
                if (instance == null) {
                    instance = new ThumbnailQueueManager();
                }
            }
        }
        return instance;
    }

    public void submitTask(Runnable task) {
        if (task == null) return;
        if (executorService.isShutdown() || executorService.isTerminated()) {
            System.err.println("ERROR: ExecutorService is shut down. Cannot submit new tasks.");
            return;
        }
        executorService.submit(task);
    }

    public void shutdown() {
        System.out.println("INFO: Shutting down ImageProcessingQueueManager...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("ERROR: ExecutorService did not terminate.");
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("INFO: ImageProcessingQueueManager shut down complete.");
    }
}