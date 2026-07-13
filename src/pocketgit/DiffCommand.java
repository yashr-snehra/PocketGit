package pocketgit;

import java.util.List;

public final class DiffCommand extends Command {

    public DiffCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        List<String> lines = repo.computeDiff();
        if (lines.isEmpty()) {
            System.out.println("No changes since the last commit.");
            return;
        }
        for (String line : lines) System.out.println(line);
    }

    @Override
    public String name() {
        return "diff";
    }
}
