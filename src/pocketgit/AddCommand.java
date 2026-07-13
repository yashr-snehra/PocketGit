package pocketgit;

import java.util.Arrays;

public final class AddCommand extends Command {

    public AddCommand(Repository repo, String[] args) {
        super(repo, args);
    }

    @Override
    public void execute() throws PocketGitException {
        if (args.length == 0)
            throw new PocketGitException("Usage: pocketgit add <path>...  (use '.' to stage everything)");
        int staged = repo.add(Arrays.asList(args));
        System.out.println("Staged " + staged + " file(s).");
    }

    @Override
    public String name() {
        return "add";
    }
}
