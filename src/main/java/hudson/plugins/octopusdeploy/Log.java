package hudson.plugins.octopusdeploy;

import hudson.model.BuildListener;
import java.io.PrintStream;

/**
 * Logs messages to the Jenkins build console.
 * @author cwetherby
 */
public class Log {
    private final BuildListener listener;
    private final PrintStream logger;
    
    /**
     * Generate a log that adds lines to the given BuildListener's console output.
     * @param listener The BuildListener responsible for adding lines to the job's console.
     */
    public Log(BuildListener listener) {
        this.listener = listener;
        this.logger = listener.getLogger();
    }
    
    /**
     * Print an info message.
     * @param msg The info message.
     */
    public void info(String msg) {
        logger.append("INFO: " + msg + "\n");
    }
    
    /**
     * Print an error message.
     * @param msg The error message.
     */
    public void error(String msg) {
        listener.error(msg);
    }
    
    /**
     * Print a fatal error message.
     * @param msg The fatal error message.
     */
    public void fatal(String msg) {
        listener.fatalError(msg);
    }
}
