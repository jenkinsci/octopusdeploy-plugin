package hudson.plugins.octopusdeploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Simple JSON sanitizer to allow special characters in JSON input. It also replaces
 * control characters (newline, tab) and replaces them with html-friendly versions
 * @author jlabroad
 */
public class JSONSanitizer {
    private static JSONSanitizer instance = null;
    
    /** Characters that can be escapes with an additional backslash */
//    private static final String[] REVERSE_SOLIDUS_ESCAPABLE_CHARS = {"\\\\", "\""};
      private static final String[] REVERSE_SOLIDUS_ESCAPABLE_CHARS = {"\""};
    private List<String> reverseSolidusEscapable = null;
    
    /** Characters that need to be replaced with something else */
    private HashMap<String, String> replacementChars = null;
    
    private JSONSanitizer() {
        reverseSolidusEscapable = new ArrayList<String>();
        reverseSolidusEscapable.addAll(Arrays.asList(REVERSE_SOLIDUS_ESCAPABLE_CHARS));
        
        replacementChars = new HashMap<String, String>();
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
        
        //Handle backslashes first
        sanitized = sanitized.replaceAll("\\\\", "\\\\u005C");        
        
        for (String ch : reverseSolidusEscapable) {
            sanitized = sanitized.replaceAll(ch, "\\\\"+ch);
        }
        
        for (Map.Entry<String, String> charPair : replacementChars.entrySet()) {
            sanitized = sanitized.replaceAll(charPair.getKey(), charPair.getValue());
        }
        
        return sanitized;        
    }
    
    @Test
    /**
     * Test the sanitizing process using a sample string
     */
    public void largeStringTest() {
        String testString = "These release notes include quotes and some special characters.\n" +
                "Consider this: \"I am a quote\" -anonymous, or \"\"I am a double-quote\" -anonymous\" -some other guy\n" +
                "Sometimes you have some \"quotes\", sometimes some other characters like ! @ # $ % ^ & * () - + = _ {} [] ~ `\n" +
                "Backslashes too: C:\\Program Files (x86)\\Jenkins\\workspace or \"C:\\Program Files (x86)\\Jenkins\\workspace\"\n" +
                "\\\\\\\\\\\\\\\\\\\\ 10 backslashes\n" +
                "	This paragraph starts with a tab. This paragraph starts with a tab. This paragraph starts with a tab.\n" +
                "This paragraph starts with a tab. This paragraph starts with a tab. This paragraph starts with a tab.\n";
        
        String sanitized = JSONSanitizer.getInstance().sanitize(testString);
        System.out.println(sanitized);
    }    
  
    /** Initiate unit tests */
    public static void main(String[] args) {
        JSONSanitizer.getInstance().largeStringTest();
    }    
}


