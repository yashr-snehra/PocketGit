package pocketgit;

import java.util.List;

public final class LogCommand extends Command {

    public LogCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        List<Commit> history = repo.history();
        if (history.isEmpty()) {
            System.out.println("No commits yet.");
            return;
        }
        String head = repo.headCommitHash();
        for (Commit c : history) {
            String marker = c.getHash().equals(head) ? "  (HEAD)" : "";
            System.out.println("commit " + c.getHash() + marker);
            if (c.getParents().size() > 1) {
                StringBuilder merges = new StringBuilder();
                for (String p : c.getParents()) merges.append(' ').append(p, 0, 7);
                System.out.println("Merge:" + merges);
            }
            System.out.println("Author: " + c.getAuthor());
            System.out.println("Date:   " + c.formattedTime());
            System.out.println();
            System.out.println("    " + c.getMessage());
            System.out.println();
        }
    }

    @Override
    public String name() {
        return "log";
    }
}
