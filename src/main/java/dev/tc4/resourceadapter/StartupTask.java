package dev.tc4.resourceadapter;

import java.util.concurrent.*;

/** The worker never calls Minecraft; the startup thread waits before pack discovery. */
public final class StartupTask<T> {
    private final CompletableFuture<T> completion = new CompletableFuture<>();
    public StartupTask(Callable<T> work) {
        Thread thread = new Thread(() -> {
            try { completion.complete(work.call()); }
            catch (Throwable error) { completion.completeExceptionally(error); }
        }, "ThaumcraftResourceAdapter worker");
        thread.setDaemon(true);
        thread.start();
    }
    public T await(Runnable renderProgress) {
        while (true) {
            try { return completion.get(50, TimeUnit.MILLISECONDS); }
            catch (TimeoutException pending) { renderProgress.run(); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while preparing local resource packs", interrupted);
            } catch (ExecutionException failure) {
                throw new IllegalStateException("Failed to scan local resource packs: " + failure.getCause().getMessage(),
                        failure.getCause());
            }
        }
    }
}
