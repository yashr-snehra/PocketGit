package pocketgit;

public final class CheckoutCommand extends Command {

    public CheckoutCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        if (args.length == 0)
            throw new PocketGitException("Usage: pocketgit checkout <branch|commit>  |  checkout -b <new-branch>");

        Repository.CheckoutResult result;
        if (args[0].equals("-b")) {
            if (args.length < 2) throw new PocketGitException("Usage: pocketgit checkout -b <new-branch>");
            result = repo.checkoutNewBranch(args[1]);
        } else {
            result = repo.checkout(args[0]);
        }

        if (result.branch != null)
            System.out.println("Switched to branch '" + result.branch + "' (" + result.commit.shortHash() + ")");
        else
            System.out.println("HEAD is now at " + result.commit.shortHash() + " " + result.commit.getMessage());
    }

    @Override
    public String name() {
        return "checkout";
    }
}
