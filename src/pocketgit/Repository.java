package pocketgit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class Repository {

    private static final String DEFAULT_BRANCH = "main";
    private static final String REF_PREFIX = "ref: refs/heads/";

    private final File root;
    private final File repoDir;
    private final File objectsDir;
    private final File refsHeadsDir;
    private final File headFile;
    private final File indexFile;
    private final ObjectStore store;
    private final Index index;

    public Repository(File root) {
        this.root = root;
        this.repoDir = new File(root, ".pocketgit");
        this.objectsDir = new File(repoDir, "objects");
        this.refsHeadsDir = new File(repoDir, "refs/heads");
        this.headFile = new File(repoDir, "HEAD");
        this.indexFile = new File(repoDir, "index");
        this.store = new ObjectStore(objectsDir);
        this.index = new Index(indexFile);
    }

    public boolean isInitialized() { return repoDir.isDirectory(); }

    public String defaultBranch() { return DEFAULT_BRANCH; }

    public void init() throws PocketGitException {
        if (isInitialized())
            throw new PocketGitException("A PocketGit repository already exists at " + repoDir.getPath());
        if (!objectsDir.mkdirs() || !refsHeadsDir.mkdirs())
            throw new PocketGitException("Could not create repository under " + repoDir.getPath());
        writeFile(headFile, REF_PREFIX + DEFAULT_BRANCH + "\n");
    }

    private void requireInit() throws PocketGitException {
        if (!isInitialized())
            throw new PocketGitException("Not a PocketGit repository. Run 'pocketgit init' first.");
    }

    public String currentBranch() throws PocketGitException {
        requireInit();
        String head = readFile(headFile).trim();
        return head.startsWith(REF_PREFIX) ? head.substring(REF_PREFIX.length()) : null;
    }

    public String headCommitHash() throws PocketGitException {
        requireInit();
        String head = readFile(headFile).trim();
        if (head.startsWith(REF_PREFIX)) {
            File ref = new File(refsHeadsDir, head.substring(REF_PREFIX.length()));
            return ref.isFile() ? readFile(ref).trim() : null;
        }
        return head.isEmpty() ? null : head;
    }

    private void updateHead(String hash) throws PocketGitException {
        String branch = currentBranch();
        if (branch != null) writeBranchRef(branch, hash);
        else writeFile(headFile, hash + "\n");
    }

    private void writeBranchRef(String name, String hash) throws PocketGitException {
        writeFile(new File(refsHeadsDir, name), hash + "\n");
    }

    private String readBranchRef(String name) throws PocketGitException {
        File ref = new File(refsHeadsDir, name);
        return ref.isFile() ? readFile(ref).trim() : null;
    }

    public boolean branchExists(String name) { return new File(refsHeadsDir, name).isFile(); }

    public List<String> listBranches() throws PocketGitException {
        requireInit();
        List<String> names = new ArrayList<>();
        File[] files = refsHeadsDir.listFiles();
        if (files != null) for (File f : files) if (f.isFile()) names.add(f.getName());
        names.sort(String::compareTo);
        return names;
    }

    public void createBranch(String name) throws PocketGitException {
        requireInit();
        if (branchExists(name)) throw new PocketGitException("Branch '" + name + "' already exists.");
        String head = headCommitHash();
        if (head == null) throw new PocketGitException("Cannot create a branch before the first commit.");
        writeBranchRef(name, head);
    }

    public int add(List<String> paths) throws PocketGitException {
        requireInit();
        index.load();
        int staged = 0;
        for (String arg : paths) {
            File f = arg.equals(".") ? root : new File(root, arg);
            if (f.isDirectory()) {
                staged += stageDirectory(f, arg);
            } else if (f.isFile()) {
                staged += stageFile(f);
            } else {
                String rel = relative(f);
                if (index.entries().containsKey(rel)) index.remove(rel);
                else throw new PocketGitException("Path '" + arg + "' did not match any file.");
            }
        }
        index.save();
        return staged;
    }

    private int stageDirectory(File dir, String arg) throws PocketGitException {
        List<File> files = new ArrayList<>();
        FileUtils.collectFiles(dir, repoDir, files);
        Set<String> present = new HashSet<>();
        int staged = 0;
        for (File f : files) {
            present.add(relative(f));
            staged += stageFile(f);
        }
        String prefix = arg.equals(".") ? "" : relative(dir) + "/";
        for (String indexed : new ArrayList<>(index.entries().keySet()))
            if (indexed.startsWith(prefix) && !present.contains(indexed)) index.remove(indexed);
        return staged;
    }

    private int stageFile(File f) throws PocketGitException {
        try {
            String hash = store.writeBlob(Files.readAllBytes(f.toPath()));
            index.stage(relative(f), hash);
            return 1;
        } catch (IOException e) {
            throw new PocketGitException("Could not read " + f.getPath() + ": " + e.getMessage(), e);
        }
    }

    public Commit commit(String message, String author) throws PocketGitException {
        requireInit();
        if (message == null || message.isBlank())
            throw new PocketGitException("A commit message is required (use -m \"message\").");
        index.load();
        Map<String, String> tree = index.entries();
        if (tree.equals(headTree()))
            throw new PocketGitException("Nothing to commit. Stage changes with 'pocketgit add' first.");
        String head = headCommitHash();
        List<String> parents = head == null ? List.of() : List.of(head);
        Commit commit = store.writeCommit(parents, author, System.currentTimeMillis(), message.trim(), tree);
        updateHead(commit.getHash());
        return commit;
    }

    public Map<String, String> headTree() throws PocketGitException {
        String head = headCommitHash();
        return head == null ? new TreeMap<>() : store.readCommit(head).getTree();
    }

    public List<Commit> history() throws PocketGitException {
        requireInit();
        List<Commit> commits = new ArrayList<>();
        String head = headCommitHash();
        if (head == null) return commits;
        Map<String, Integer> distance = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(head);
        distance.put(head, 0);
        while (!queue.isEmpty()) {
            String h = queue.poll();
            Commit c = store.readCommit(h);
            commits.add(c);
            for (String p : c.getParents())
                if (!distance.containsKey(p)) { distance.put(p, distance.get(h) + 1); queue.add(p); }
        }
        commits.sort((a, b) -> {
            int d = Integer.compare(distance.get(a.getHash()), distance.get(b.getHash()));
            if (d != 0) return d;
            int t = Long.compare(b.getTimestamp(), a.getTimestamp());
            return t != 0 ? t : a.getHash().compareTo(b.getHash());
        });
        return commits;
    }

    public CheckoutResult checkout(String ref) throws PocketGitException {
        requireInit();
        if (computeStatus().hasTrackedChanges())
            throw new PocketGitException("You have uncommitted changes to tracked files. Commit them first.");
        Map<String, String> fromTree = headTree();
        String branch = null;
        String targetHash;
        if (branchExists(ref)) {
            branch = ref;
            targetHash = readBranchRef(ref);
        } else {
            targetHash = store.exists(ref) ? ref : store.resolve(ref);
            if (targetHash == null) throw new PocketGitException("No branch or commit matches '" + ref + "'.");
        }
        Commit target = store.readCommit(targetHash);
        updateWorkingTree(fromTree, target.getTree());
        index.set(target.getTree());
        index.save();
        if (branch != null) writeFile(headFile, REF_PREFIX + branch + "\n");
        else writeFile(headFile, targetHash + "\n");
        return new CheckoutResult(branch, target);
    }

    public CheckoutResult checkoutNewBranch(String name) throws PocketGitException {
        createBranch(name);
        return checkout(name);
    }

    public MergeResult merge(String branchName, String author) throws PocketGitException {
        requireInit();
        if (computeStatus().hasTrackedChanges())
            throw new PocketGitException("You have uncommitted changes to tracked files. Commit them first.");
        String ours = headCommitHash();
        if (ours == null) throw new PocketGitException("No commits yet - nothing to merge into.");
        if (!branchExists(branchName)) throw new PocketGitException("Branch '" + branchName + "' does not exist.");
        String theirs = readBranchRef(branchName);

        if (theirs.equals(ours) || isAncestor(theirs, ours)) return MergeResult.upToDate();

        Map<String, String> ourTree = store.readCommit(ours).getTree();

        if (isAncestor(ours, theirs)) {
            Commit target = store.readCommit(theirs);
            updateWorkingTree(ourTree, target.getTree());
            index.set(target.getTree());
            index.save();
            updateHead(theirs);
            return MergeResult.fastForward(target);
        }

        String base = mergeBase(ours, theirs);
        Map<String, String> baseTree = base == null ? new TreeMap<>() : store.readCommit(base).getTree();
        Map<String, String> theirTree = store.readCommit(theirs).getTree();

        Map<String, String> merged = new TreeMap<>();
        List<String> conflicts = new ArrayList<>();
        Set<String> paths = new TreeSet<>();
        paths.addAll(baseTree.keySet());
        paths.addAll(ourTree.keySet());
        paths.addAll(theirTree.keySet());
        for (String path : paths) {
            String o = ourTree.get(path), t = theirTree.get(path), b = baseTree.get(path);
            if (Objects.equals(o, t)) { if (o != null) merged.put(path, o); }
            else if (Objects.equals(o, b)) { if (t != null) merged.put(path, t); }
            else if (Objects.equals(t, b)) { if (o != null) merged.put(path, o); }
            else conflicts.add(path);
        }
        if (!conflicts.isEmpty()) return MergeResult.conflict(conflicts);

        Commit commit = store.writeCommit(List.of(ours, theirs), author, System.currentTimeMillis(),
                "Merge branch '" + branchName + "'", merged);
        updateWorkingTree(ourTree, merged);
        index.set(merged);
        index.save();
        updateHead(commit.getHash());
        return MergeResult.merged(commit);
    }

    private boolean isAncestor(String candidate, String of) throws PocketGitException {
        return ancestors(of).contains(candidate);
    }

    private Set<String> ancestors(String hash) throws PocketGitException {
        Set<String> seen = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(hash);
        seen.add(hash);
        while (!queue.isEmpty())
            for (String p : store.readCommit(queue.poll()).getParents())
                if (seen.add(p)) queue.add(p);
        return seen;
    }

    private String mergeBase(String a, String b) throws PocketGitException {
        Set<String> ancestorsA = ancestors(a);
        Set<String> seen = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(b);
        seen.add(b);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (ancestorsA.contains(cur)) return cur;
            for (String p : store.readCommit(cur).getParents()) if (seen.add(p)) queue.add(p);
        }
        return null;
    }

    private void updateWorkingTree(Map<String, String> from, Map<String, String> to) throws PocketGitException {
        try {
            for (Map.Entry<String, String> e : to.entrySet()) {
                File target = new File(root, e.getKey());
                File parent = target.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs())
                    throw new PocketGitException("Could not create directory " + parent.getPath());
                Files.write(target.toPath(), store.read(e.getValue()));
            }
            for (String path : from.keySet())
                if (!to.containsKey(path)) new File(root, path).delete();
        } catch (IOException e) {
            throw new PocketGitException("Could not update working tree: " + e.getMessage(), e);
        }
    }

    public List<String> computeDiff() throws PocketGitException {
        requireInit();
        Map<String, String> head = headTree();
        Map<String, String> work = workingTree();
        Set<String> paths = new TreeSet<>();
        paths.addAll(head.keySet());
        paths.addAll(work.keySet());
        List<String> out = new ArrayList<>();
        for (String path : paths) {
            String oh = head.get(path), wh = work.get(path);
            if (Objects.equals(oh, wh)) continue;
            byte[] oldBytes = oh != null ? store.read(oh) : new byte[0];
            byte[] newBytes = wh != null ? readWorkBytes(path) : new byte[0];
            out.add("--- diff: " + path + " ---");
            if (FileUtils.isBinary(oldBytes) || FileUtils.isBinary(newBytes)) out.add("Binary files differ");
            else out.addAll(DiffUtil.diff(splitLines(oldBytes), splitLines(newBytes)));
            out.add("");
        }
        return out;
    }

    public Status computeStatus() throws PocketGitException {
        requireInit();
        index.load();
        Map<String, String> idx = index.entries();
        Map<String, String> head = headTree();
        Map<String, String> work = workingTree();

        Status s = new Status();
        s.branch = currentBranch();
        s.detachedHash = s.branch == null ? headCommitHash() : null;

        for (Map.Entry<String, String> e : idx.entrySet()) {
            String h = head.get(e.getKey());
            if (h == null) s.stagedNew.add(e.getKey());
            else if (!h.equals(e.getValue())) s.stagedModified.add(e.getKey());
        }
        for (String p : head.keySet()) if (!idx.containsKey(p)) s.stagedDeleted.add(p);

        for (Map.Entry<String, String> e : work.entrySet()) {
            if (idx.containsKey(e.getKey())) {
                if (!idx.get(e.getKey()).equals(e.getValue())) s.unstagedModified.add(e.getKey());
            } else {
                s.untracked.add(e.getKey());
            }
        }
        for (String p : idx.keySet()) if (!work.containsKey(p)) s.unstagedDeleted.add(p);
        return s;
    }

    private Map<String, String> workingTree() throws PocketGitException {
        List<File> files = new ArrayList<>();
        FileUtils.collectFiles(root, repoDir, files);
        Map<String, String> map = new TreeMap<>();
        try {
            for (File f : files)
                map.put(relative(f), FileUtils.sha1(Files.readAllBytes(f.toPath())));
        } catch (IOException e) {
            throw new PocketGitException("Could not read working tree: " + e.getMessage(), e);
        }
        return map;
    }

    private byte[] readWorkBytes(String path) throws PocketGitException {
        try {
            return Files.readAllBytes(new File(root, path).toPath());
        } catch (IOException e) {
            throw new PocketGitException("Could not read " + path + ": " + e.getMessage(), e);
        }
    }

    private String relative(File f) { return FileUtils.relativePath(root, f); }

    private static List<String> splitLines(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.isEmpty()) return List.of();
        if (text.endsWith("\n")) text = text.substring(0, text.length() - 1);
        return Arrays.asList(text.split("\n", -1));
    }

    private static String readFile(File f) throws PocketGitException {
        try {
            return Files.readString(f.toPath());
        } catch (IOException e) {
            throw new PocketGitException("Could not read " + f.getPath() + ": " + e.getMessage(), e);
        }
    }

    private static void writeFile(File f, String content) throws PocketGitException {
        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            Files.writeString(f.toPath(), content);
        } catch (IOException e) {
            throw new PocketGitException("Could not write " + f.getPath() + ": " + e.getMessage(), e);
        }
    }

    public static final class Status {
        public String branch;
        public String detachedHash;
        public final List<String> stagedNew = new ArrayList<>();
        public final List<String> stagedModified = new ArrayList<>();
        public final List<String> stagedDeleted = new ArrayList<>();
        public final List<String> unstagedModified = new ArrayList<>();
        public final List<String> unstagedDeleted = new ArrayList<>();
        public final List<String> untracked = new ArrayList<>();

        public boolean isClean() {
            return !hasTrackedChanges() && untracked.isEmpty();
        }

        public boolean hasTrackedChanges() {
            return !(stagedNew.isEmpty() && stagedModified.isEmpty() && stagedDeleted.isEmpty()
                    && unstagedModified.isEmpty() && unstagedDeleted.isEmpty());
        }
    }

    public static final class CheckoutResult {
        public final String branch;
        public final Commit commit;
        public CheckoutResult(String branch, Commit commit) { this.branch = branch; this.commit = commit; }
    }

    public static final class MergeResult {
        public enum Kind { UP_TO_DATE, FAST_FORWARD, MERGED, CONFLICT }
        public final Kind kind;
        public final Commit commit;
        public final List<String> conflicts;

        private MergeResult(Kind kind, Commit commit, List<String> conflicts) {
            this.kind = kind;
            this.commit = commit;
            this.conflicts = conflicts;
        }

        static MergeResult upToDate() { return new MergeResult(Kind.UP_TO_DATE, null, List.of()); }
        static MergeResult fastForward(Commit c) { return new MergeResult(Kind.FAST_FORWARD, c, List.of()); }
        static MergeResult merged(Commit c) { return new MergeResult(Kind.MERGED, c, List.of()); }
        static MergeResult conflict(List<String> c) { return new MergeResult(Kind.CONFLICT, null, c); }
    }
}
