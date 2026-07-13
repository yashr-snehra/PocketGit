package pocketgit;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

final class FileUtils {

    private FileUtils() { }

    static void collectFiles(File dir, File repoDir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.equals(repoDir)) continue;
            if (child.isDirectory()) collectFiles(child, repoDir, out);
            else out.add(child);
        }
    }

    static String sha1(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available in this JVM", e);
        }
    }

    static String relativePath(File root, File file) {
        return root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
    }

    static boolean isBinary(byte[] data) {
        int limit = Math.min(data.length, 8000);
        for (int i = 0; i < limit; i++) if (data[i] == 0) return true;
        return false;
    }
}
