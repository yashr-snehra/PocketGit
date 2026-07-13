package pocketgit;

import java.util.List;

public final class StatusCommand extends Command {

    public StatusCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        Repository.Status s = repo.computeStatus();
        if (s.branch != null) System.out.println("On branch " + s.branch);
        else System.out.println("HEAD detached at " + shorten(s.detachedHash));

        if (s.isClean()) {
            System.out.println("nothing to commit, working tree clean");
            return;
        }

        if (!s.stagedNew.isEmpty() || !s.stagedModified.isEmpty() || !s.stagedDeleted.isEmpty()) {
            System.out.println();
            System.out.println("Changes to be committed:");
            print("new file", s.stagedNew);
            print("modified", s.stagedModified);
            print("deleted", s.stagedDeleted);
        }
        if (!s.unstagedModified.isEmpty() || !s.unstagedDeleted.isEmpty()) {
            System.out.println();
            System.out.println("Changes not staged for commit:");
            print("modified", s.unstagedModified);
            print("deleted", s.unstagedDeleted);
        }
        if (!s.untracked.isEmpty()) {
            System.out.println();
            System.out.println("Untracked files:");
            for (String p : s.untracked) System.out.println("        " + p);
        }
    }

    private static void print(String tag, List<String> paths) {
        for (String p : paths) System.out.printf("        %-9s %s%n", tag + ":", p);
    }

    private static String shorten(String hash) {
        return hash == null ? "(none)" : hash.substring(0, Math.min(7, hash.length()));
    }

    @Override
    public String name() {
        return "status";
    }
}
