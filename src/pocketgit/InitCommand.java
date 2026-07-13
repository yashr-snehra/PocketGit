package pocketgit;

public final class InitCommand extends Command {

    public InitCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        repo.init();
        System.out.println("Initialized empty PocketGit repository in .pocketgit/ (branch '"
                + repo.defaultBranch() + "')");
    }

    @Override
    public String name() {
        return "init";
    }
}
