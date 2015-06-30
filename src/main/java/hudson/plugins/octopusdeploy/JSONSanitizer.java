package hudson.plugins.octopusdeploy;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple JSON sanitizer to allow special characters in JSON input. It also replaces
 * control characters (newline, tab) and replaces them with html-friendly versions
 * @author jlabroad
 */
public class JSONSanitizer {
    private static JSONSanitizer instance = null;
    
    /** Characters that need to be replaced with something else */
    private HashMap<String, String> replacementChars = null;
    
    private JSONSanitizer() {
        replacementChars = new HashMap<String, String>();
        replacementChars.put("\"", "\\\\\"");
        replacementChars.put("\n", "<br/>"); //Replace new line with html line break
        replacementChars.put("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"); //Replace tab with 4 spaces
    }
    
    public static JSONSanitizer getInstance() {
        if (instance == null ) {
            instance = new JSONSanitizer();
        }
        return instance;
    }
    
    /**
     * Sanitizes the input string so that it can be represented in JSON
     * @param dirtyString The un-sanitized string
     * @return The sanitized string that can be directly added to a JSON command
     */
    public String sanitize(String dirtyString) {
        String sanitized = dirtyString;
        
        // Handle backslashes first. All backslashes that remain after this are for escaping purposes
        sanitized = sanitized.replaceAll("\\\\", "\\\\u005C");        
        
        // Make all the replacements
        for (Map.Entry<String, String> charPair : replacementChars.entrySet()) {
            sanitized = sanitized.replaceAll(charPair.getKey(), charPair.getValue());
        }
        
        return sanitized;        
    }    
}

