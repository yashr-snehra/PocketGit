package pocketgit;

public final class CommandFactory {

    private CommandFactory() { }

    public static Command create(String name, Repository repo, String[] args) throws PocketGitException {
        return switch (name) {
            case "init"     -> new InitCommand(repo, args);
            case "add"      -> new AddCommand(repo, args);
            case "status"   -> new StatusCommand(repo, args);
            case "commit"   -> new CommitCommand(repo, args);
            case "log"      -> new LogCommand(repo, args);
            case "diff"     -> new DiffCommand(repo, args);
            case "branch"   -> new BranchCommand(repo, args);
            case "checkout" -> new CheckoutCommand(repo, args);
            case "merge"    -> new MergeCommand(repo, args);
            default -> throw new PocketGitException("Unknown command '" + name + "'. Run 'pocketgit help'.");
        };
    }
}
