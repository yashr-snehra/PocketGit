package pocketgit;

import java.util.List;

public final class BranchCommand extends Command {

    public BranchCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        if (args.length == 0) {
            String current = repo.currentBranch();
            List<String> branches = repo.listBranches();
            if (branches.isEmpty()) {
                System.out.println("No branches yet (make your first commit).");
                return;
            }
            for (String b : branches) System.out.println((b.equals(current) ? "* " : "  ") + b);
        } else {
            repo.createBranch(args[0]);
            System.out.println("Created branch '" + args[0] + "'.");
        }
    }

    @Override
    public String name() {
        return "branch";
    }
}
