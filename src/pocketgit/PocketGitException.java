package pocketgit;

public class PocketGitException extends Exception {

    public PocketGitException(String message) {
        super(message);
    }

    public PocketGitException(String message, Throwable cause) {
        super(message, cause);
    }
}
