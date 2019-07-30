package hudson.plugins.octopusdeploy.exception;

/**
 * Represents an exception throw while working with resources
 */
public class ResourceException extends RuntimeException {
    public ResourceException() {
        super();
    }

    public ResourceException(final String message) {
        super(message);
    }

    public ResourceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ResourceException(final Throwable cause) {
        super(cause);
    }

    protected ResourceException(final String message, final Throwable cause,
                                final boolean enableSuppression,
                                final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
