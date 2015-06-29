/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.octopusdeploy;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jlabroad
 */
public class JSONSanitizerTest {
    
    public JSONSanitizerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of sanitize method, of class JSONSanitizer.
     */
    @Test
    public void testLargeString() {
        String testString = "These release notes include quotes and some special characters.\n" +
                "Consider this: \"I am a quote\" -anonymous, or \"\"I am a double-quote\" -anonymous\" -some other guy\n" +
                "Sometimes you have some \"quotes\", sometimes some other characters like ! @ # $ % ^ & * () - + = _ {} [] ~ `\n" +
                "Backslashes too: C:\\Program Files (x86)\\Jenkins\\workspace or \"C:\\Program Files (x86)\\Jenkins\\workspace\"\n" +
                "\\\\ 2 backslashes\n" +
                "	This paragraph starts with a tab. This paragraph starts with a tab. This paragraph starts with a tab.\n" +
                "This paragraph starts with a tab. This paragraph starts with a tab. This paragraph starts with a tab.\n";
        
        final String answer = "These release notes include quotes and some special characters.<br/>" +
                "Consider this: \\\"I am a quote\\\" -anonymous, or \\\"\\\"I am a double-quote\\\" -anonymous\\\" -some other guy<br/>" +
                "Sometimes you have some \\\"quotes\\\", sometimes some other characters like ! @ # $ % ^ & * () - + = _ {} [] ~ `<br/>" +
                "Backslashes too: C:\\u005CProgram Files (x86)\\u005CJenkins\\u005Cworkspace or \\\"C:\\u005CProgram Files (x86)\\u005CJenkins\\u005Cworkspace\\\"<br/>\\u005C\\u005C 2 backslashes<br/>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;This paragraph starts with a tab. This paragraph starts with a tab. " +
                "This paragraph starts with a tab.<br/>This paragraph starts with a tab. This paragraph starts with a tab. This paragraph starts with a tab.<br/>";
        
        String sanitized = JSONSanitizer.getInstance().sanitize(testString);
        assertEquals(sanitized.equals(answer), true);
    }         
 }
