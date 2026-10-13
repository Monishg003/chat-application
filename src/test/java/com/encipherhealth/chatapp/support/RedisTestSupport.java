package com.encipherhealth.chatapp.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public final class RedisTestSupport
{
    private Process process;
    private Path dataDir;
    private int port;

    public void start()
    {
        if (process != null && process.isAlive()) {
            return;
        }
        try {
            port = freePort();
            dataDir = Files.createTempDirectory("chat-app-redis");
            process = new ProcessBuilder(
                    "redis-server",
                    "--port", String.valueOf(port),
                    "--bind", "127.0.0.1",
                    "--dir", dataDir.toAbsolutePath().toString(),
                    "--dbfilename", "dump.rdb",
                    "--appendonly", "yes",
                    "--appendfilename", "appendonly.aof",
                    "--save", "60", "1"
            ).redirectErrorStream(true).start();
            waitUntilReady();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to start redis-server for tests", e);
        }
    }

    public void stop()
    {
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            process = null;
        }
    }

    public int port()
    {
        return port;
    }

    private void waitUntilReady()
    {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                throw new IllegalStateException("redis-server exited before becoming ready");
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return;
            }
            catch (IOException ignored) {
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for redis-server", e);
                }
            }
        }
        throw new IllegalStateException("Timed out waiting for redis-server on port " + port);
    }

    private int freePort()
            throws IOException
    {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
