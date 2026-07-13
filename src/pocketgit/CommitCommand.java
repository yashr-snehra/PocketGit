package pocketgit;

public final class CommitCommand extends Command {

    public CommitCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        String message = parseMessage(args);
        String author = System.getProperty("user.name", "unknown");
        Commit commit = repo.commit(message, author);
        String branch = repo.currentBranch();
        String where = branch != null ? branch : "detached";
        System.out.println("[" + where + " " + commit.shortHash() + "] " + commit.getMessage());
    }

    private static String parseMessage(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            if (arg.equals("-m") || arg.equals("--message")) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(arg);
        }
        return sb.toString().trim();
    }

    @Override
    public String name() {
        return "commit";
    }
}
