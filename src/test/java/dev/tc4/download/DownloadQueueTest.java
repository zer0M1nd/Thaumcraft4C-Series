package dev.tc4.download;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class DownloadQueueTest {
    private static final URI FIRST = URI.create("https://example.org/first"),
            SECOND = URI.create("https://example.org/second");

    @Test
    void noTransferBeforeRenderPauseResumesSameStreamAndFallbackExhaustsInOrder() throws Exception {
        var calls = new CopyOnWriteArrayList<URI>();
        var committed = new AtomicInteger();
        var atProgress = new CountDownLatch(1);
        var nextChunk = new CountDownLatch(1);
        var task =
                new DownloadQueue.Task() {
                    public DownloadQueue.Item item() {
                        return new DownloadQueue.Item(
                                "a",
                                "A",
                                "1",
                                "mods",
                                DownloadQueue.Kind.UPDATE,
                                List.of(FIRST, SECOND));
                    }

                    public DownloadQueue.Prepared local() {
                        return null;
                    }

                    public DownloadQueue.Prepared download(URI u, DownloadQueue.Progress p)
                            throws IOException {
                        calls.add(u);
                        if (u.equals(FIRST)) throw new IOException("Unavailable");
                        p.bytes(30, 100);
                        atProgress.countDown();
                        try {
                            if (!nextChunk.await(3, TimeUnit.SECONDS))
                                throw new IOException("No next chunk");
                        } catch (InterruptedException e) {
                            throw new InterruptedIOException();
                        }
                        p.bytes(75, 100);
                        return () -> {
                            committed.incrementAndGet();
                            return "";
                        };
                    }
                };
        try (var queue = new DownloadQueue(List.of(task))) {
            assertTrue(calls.isEmpty());
            assertFalse(queue.ready());
            queue.action("shown", "");
            assertTrue(atProgress.await(2, TimeUnit.SECONDS));
            queue.action("pause", "a");
            nextChunk.countDown();
            Thread.sleep(80);
            var row =
                    JsonParser.parseString(queue.snapshot())
                            .getAsJsonObject()
                            .getAsJsonArray("rows")
                            .get(0)
                            .getAsJsonObject();
            assertEquals("PAUSED", row.get("state").getAsString());
            assertEquals(30, row.get("received").getAsLong());
            assertEquals(0, committed.get());
            queue.action("resume", "a");
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (!queue.ready() && System.nanoTime() < end) Thread.sleep(5);
            assertTrue(queue.ready());
            assertEquals(1, committed.get());
            assertEquals(List.of(FIRST, SECOND), calls);
        }
    }

    @Test
    void cancellingRequiredResourceKeepsContinueLockedAndDiscardsLateResult() throws Exception {
        var start = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        var closed = new CountDownLatch(1);
        var task =
                new DownloadQueue.Task() {
                    public DownloadQueue.Item item() {
                        return new DownloadQueue.Item(
                                "a",
                                "A",
                                "1",
                                "thaumcraft",
                                DownloadQueue.Kind.RESOURCE,
                                List.of(FIRST));
                    }

                    public DownloadQueue.Prepared local() {
                        return null;
                    }

                    public DownloadQueue.Prepared download(URI u, DownloadQueue.Progress p)
                            throws IOException {
                        start.countDown();
                        try {
                            finish.await(2, TimeUnit.SECONDS);
                        } catch (InterruptedException ex) {
                            throw new InterruptedIOException();
                        }
                        return new DownloadQueue.Prepared() {
                            public String apply() {
                                fail("Cancelled transfer must not publish");
                                return "";
                            }

                            public void close() {
                                closed.countDown();
                            }
                        };
                    }
                };
        try (var queue = new DownloadQueue(List.of(task))) {
            queue.action("shown", "");
            assertTrue(start.await(2, TimeUnit.SECONDS));
            queue.action("cancel", "a");
            finish.countDown();
            assertTrue(closed.await(2, TimeUnit.SECONDS));
            queue.action("skip", "a");
            assertFalse(queue.ready());
            assertTrue(queue.snapshot().contains("CANCELLED"));
        }
    }
}
