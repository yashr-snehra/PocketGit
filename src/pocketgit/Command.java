package pocketgit;

public abstract class Command {

    protected final Repository repo;
    protected final String[] args;

    protected Command(Repository repo, String[] args) {
        this.repo = repo;
        this.args = args;
    }

    public abstract void execute() throws PocketGitException;

    public abstract String name();
}
