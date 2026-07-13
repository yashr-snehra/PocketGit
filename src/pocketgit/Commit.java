package pocketgit;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Commit implements Comparable<Commit> {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final String hash;
    private final List<String> parents;
    private final String author;
    private final long timestamp;
    private final String message;
    private final Map<String, String> tree;

    public Commit(String hash, List<String> parents, String author, long timestamp,
                  String message, Map<String, String> tree) {
        this.hash = hash;
        this.parents = List.copyOf(parents);
        this.author = author;
        this.timestamp = timestamp;
        this.message = message;
        this.tree = new TreeMap<>(tree);
    }

    public String getHash() { return hash; }
    public List<String> getParents() { return parents; }
    public String getAuthor() { return author; }
    public long getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
    public Map<String, String> getTree() { return Collections.unmodifiableMap(tree); }

    public String shortHash() { return hash.substring(0, Math.min(7, hash.length())); }

    public String formattedTime() { return TIME_FORMAT.format(Instant.ofEpochMilli(timestamp)); }

    @Override
    public int compareTo(Commit other) { return Long.compare(this.timestamp, other.timestamp); }

    @Override
    public String toString() { return shortHash() + " " + message; }
}
