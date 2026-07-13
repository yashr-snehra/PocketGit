package pocketgit;

import java.io.File;
import java.util.Arrays;

public final class Main {

    public static void main(String[] args) {
        if (args.length == 0 || isHelp(args[0])) {
            printUsage();
            return;
        }
        String name = args[0];
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        Repository repo = new Repository(new File(System.getProperty("user.dir")));
        try {
            CommandFactory.create(name, repo, rest).execute();
        } catch (PocketGitException e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static boolean isHelp(String arg) {
        return arg.equals("help") || arg.equals("-h") || arg.equals("--help");
    }

    private static void printUsage() {
        System.out.println("""
                PocketGit - a tiny Git-like version control system

                Usage: java -cp out pocketgit.Main <command> [args]

                Commands:
                  init                  Start a repository in the current folder
                  add <path>...         Stage files for the next commit ('.' = all)
                  status                Show staged / unstaged / untracked changes
                  commit -m "message"   Save the staged files as a new commit
                  log                   List past commits (newest first)
                  diff                  Show line changes since the last commit
                  branch [name]         List branches, or create one
                  checkout <target>     Switch to a branch or commit (-b <name> = create + switch)
                  merge <branch>        Merge another branch into the current one
                  help                  Show this help
                """);
    }
}
