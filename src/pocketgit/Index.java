package pocketgit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

public final class Index {

    private final File indexFile;
    private final Map<String, String> entries = new TreeMap<>();

    public Index(File indexFile) {
        this.indexFile = indexFile;
    }

    public void load() throws PocketGitException {
        entries.clear();
        if (!indexFile.isFile()) return;
        try {
            for (String line : Files.readAllLines(indexFile.toPath())) {
                int tab = line.indexOf('\t');
                if (tab > 0) entries.put(line.substring(tab + 1), line.substring(0, tab));
            }
        } catch (IOException e) {
            throw new PocketGitException("Could not read index: " + e.getMessage(), e);
        }
    }

    public void save() throws PocketGitException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : entries.entrySet())
            sb.append(e.getValue()).append('\t').append(e.getKey()).append('\n');
        try {
            Files.writeString(indexFile.toPath(), sb.toString());
        } catch (IOException e) {
            throw new PocketGitException("Could not write index: " + e.getMessage(), e);
        }
    }

    public Map<String, String> entries() {
        return new TreeMap<>(entries);
    }

    public void stage(String path, String hash) {
        entries.put(path, hash);
    }

    public void remove(String path) {
        entries.remove(path);
    }

    public void set(Map<String, String> all) {
        entries.clear();
        entries.putAll(all);
    }
}
