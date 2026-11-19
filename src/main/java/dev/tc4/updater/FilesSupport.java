package dev.tc4.updater;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import static java.nio.file.StandardOpenOption.*;

public final class FilesSupport {
    private FilesSupport() {}
    public static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    public static String sha256(Path file) throws IOException {
        var digest = digest();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536];
            for (int count; (count = in.read(buffer)) != -1;) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    public static String sha256(byte[] bytes) { return HexFormat.of().formatHex(digest().digest(bytes)); }
    public static byte[] readBounded(InputStream input, int limit) throws IOException {
        byte[] bytes = input.readNBytes(limit + 1);
        if (bytes.length > limit) throw new IOException("Input exceeds " + limit + " bytes");
        return bytes;
    }
    public static void writeAtomic(Path target, byte[] bytes) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".pending");
        try (FileChannel channel = FileChannel.open(temporary, CREATE, TRUNCATE_EXISTING, WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    public static String filename(String name) throws IOException {
        if (name == null || name.length() > 200 || !name.matches("[A-Za-z0-9][A-Za-z0-9_.+\\-]*\\.jar"))
            throw new IOException("Invalid JAR filename: " + name);
        return name;
    }
    public static String localFilename(String name) throws IOException {
        if (name == null || name.length() > 255 || name.isBlank()
                || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")
                || name.matches(".*[\\\\/:*?\"<>|\\x00-\\x1f].*") || name.equals(".") || name.equals(".."))
            throw new IOException("Invalid installed JAR filename: " + name);
        return name;
    }
    public static void regularFile(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Expected regular file: " + path);
    }
}
