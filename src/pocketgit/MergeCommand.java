package pocketgit;

public final class MergeCommand extends Command {

    public MergeCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        if (args.length == 0) throw new PocketGitException("Usage: pocketgit merge <branch>");
        String author = System.getProperty("user.name", "unknown");
        Repository.MergeResult r = repo.merge(args[0], author);
        switch (r.kind) {
            case UP_TO_DATE -> System.out.println("Already up to date.");
            case FAST_FORWARD -> System.out.println("Fast-forward to " + r.commit.shortHash() + ".");
            case MERGED -> System.out.println("Merge made a new commit " + r.commit.shortHash() + ".");
            case CONFLICT -> {
                System.out.println("Merge aborted: these files changed on both branches:");
                for (String p : r.conflicts) System.out.println("        " + p);
                System.out.println("PocketGit merges whole files only. Reconcile them by hand, then commit.");
            }
        }
    }

    @Override
    public String name() {
        return "merge";
    }
}
