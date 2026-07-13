package pocketgit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ObjectStore {

    private final File objectsDir;
    private final Map<String, Commit> commitCache = new HashMap<>();

    public ObjectStore(File objectsDir) {
        this.objectsDir = objectsDir;
    }

    public String writeBlob(byte[] data) throws PocketGitException {
        String hash = FileUtils.sha1(data);
        writeObject(hash, data);
        return hash;
    }

    public byte[] read(String hash) throws PocketGitException {
        File f = new File(objectsDir, hash);
        if (!f.isFile()) throw new PocketGitException("Missing object " + hash);
        try {
            return Files.readAllBytes(f.toPath());
        } catch (IOException e) {
            throw new PocketGitException("Could not read object " + hash + ": " + e.getMessage(), e);
        }
    }

    public boolean exists(String hash) {
        return new File(objectsDir, hash).isFile();
    }

    public String resolve(String prefix) throws PocketGitException {
        if (prefix == null || prefix.isEmpty()) return null;
        File[] files = objectsDir.listFiles();
        String match = null;
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(prefix)) {
                    if (match != null) throw new PocketGitException("Short id '" + prefix + "' is ambiguous.");
                    match = f.getName();
                }
            }
        }
        return match;
    }

    public Commit writeCommit(List<String> parents, String author, long timestamp,
                              String message, Map<String, String> tree) throws PocketGitException {
        byte[] body = encode(parents, author, timestamp, message, tree).getBytes(StandardCharsets.UTF_8);
        String hash = FileUtils.sha1(body);
        writeObject(hash, body);
        Commit commit = new Commit(hash, parents, author, timestamp, message, tree);
        commitCache.put(hash, commit);
        return commit;
    }

    public Commit readCommit(String hash) throws PocketGitException {
        Commit cached = commitCache.get(hash);
        if (cached != null) return cached;
        String text = new String(read(hash), StandardCharsets.UTF_8);
        List<String> parents = new ArrayList<>();
        String author = "";
        long timestamp = 0L;
        String message = "";
        Map<String, String> tree = new TreeMap<>();
        boolean inTree = false;
        for (String line : text.split("\n", -1)) {
            if (inTree) {
                int tab = line.indexOf('\t');
                if (tab > 0) tree.put(line.substring(tab + 1), line.substring(0, tab));
            } else if (line.equals("tree")) {
                inTree = true;
            } else if (line.startsWith("parent ")) {
                parents.add(line.substring(7));
            } else if (line.startsWith("author ")) {
                author = line.substring(7);
            } else if (line.startsWith("timestamp ")) {
                timestamp = Long.parseLong(line.substring(10).trim());
            } else if (line.startsWith("message ")) {
                message = line.substring(8);
            }
        }
        Commit commit = new Commit(hash, parents, author, timestamp, message, tree);
        commitCache.put(hash, commit);
        return commit;
    }

    private static String encode(List<String> parents, String author, long timestamp,
                                 String message, Map<String, String> tree) {
        StringBuilder sb = new StringBuilder();
        for (String p : parents) sb.append("parent ").append(p).append('\n');
        sb.append("author ").append(author).append('\n');
        sb.append("timestamp ").append(timestamp).append('\n');
        sb.append("message ").append(message.replace('\n', ' ')).append('\n');
        sb.append("tree").append('\n');
        for (Map.Entry<String, String> e : new TreeMap<>(tree).entrySet())
            sb.append(e.getValue()).append('\t').append(e.getKey()).append('\n');
        return sb.toString();
    }

    private void writeObject(String hash, byte[] data) throws PocketGitException {
        File f = new File(objectsDir, hash);
        if (f.exists()) return;
        try {
            Files.write(f.toPath(), data);
        } catch (IOException e) {
            throw new PocketGitException("Could not write object " + hash + ": " + e.getMessage(), e);
        }
    }
}
