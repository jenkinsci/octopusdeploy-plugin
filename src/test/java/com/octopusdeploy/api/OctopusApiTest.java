package com.octopusdeploy.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * OctopusApi tests
 * @author jlabroad
 */
public class OctopusApiTest {
    
    private Random rand = null;
    private Set<Character> forbiddenChars = null;
    
    public OctopusApiTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        rand = new Random();
        
        //These characters cannot be in error messages.
        forbiddenChars = new HashSet<Character>();
        forbiddenChars.add('\"');
        forbiddenChars.add('\\');
        forbiddenChars.add(']');        
    }
    
    @After
    public void tearDown() {
    }

     /**
     * Test of getErrorsFromResponse method, of class OctopusApi. Using a single sample Octopus response
     */
    @Test
    public void testGetErrorsFromResponseStatic() {
        System.out.println("getErrorsFromResponse");
        String errMsg1 = "No package version was specified for the step 'test nuget'";
        String errMsg2 = "Error msg number 2";
        String response = String.format("<!DOCTYPE html><html lang=\"en\"><head>  <title ng-bind=\"$root.pageTitle + ' - Octopus Deploy'\">Octopus Deploy</title>    <link rel=\"stylesheet\" href=\"/css/octopus.min.css\" />    <link rel=\"apple-touch-icon\" href=\"/img/icons/Octopus-96x96.png\" />  <link rel=\"icon\" href=\"/img/icons/Octopus-96x96.png\" />  <!--[if IE]><link rel=\"shortcut icon\" href=\"img/icons/Octopus-16x16.ico\"><![endif]-->  <meta name=\"msapplication-TileColor\" content=\"#2F93E0\">  <meta name=\"msapplication-TileImage\" content=\"/img/icons/Octopus-144x144-Transparent.png\">  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">  <meta name=\"viewport\" content=\"width=device-width\">        <script type=\"text/javascript\" src=\"/octopus.min.js\"></script>        <script>    $(function() {        var errorData = {\"title\":\"Bad request\",\"message\":\"There was a problem with your request.\",\"details\":{\"ErrorMessage\":\"There was a problem with your request.\",\"Errors\":[\"%s\", \"%s\"]}};        $(\"#err-title\").text(errorData.title);        $(\"#err-message\").text(errorData.message);        $(\"#err-details\").text(angular.toJson(errorData.details, true));        $(\"#show-details\").click(function() {            $(\"#show-details\").hide();            $(\"#err-details\").show();         });        $(\"#connection-failed\").show();    });    </script></head><body><div id='initial-page-loader'>  <div class='container'>    <div class='narrow'>      <div class='box-shadow'>        <div class='pad30'>          <div class='connection-failed'>            <div class=\"clippy\">                <img src=\"/img/layout/octoclippy.png\" width=\"64\" height=\"64\" alt=\"Octoclippy is here to help!\" title=\"Octoclippy is here to help!\" />                <div class=\"clippy-says\">                    <h2 id='err-title'>Oops!</h2>                    <p id='err-message'>Something went wrong...</p>                    <a class='btn btn-info' id='show-details'>Show details</a>                    <pre id='err-details' style='display: none'></pre>                </div>            </div>          </div>        </div>      </div>    </div>  </div></div></body></html>",
                errMsg1, errMsg2);
        String result = OctopusApi.getErrorsFromResponse(response);
        assertEquals(result.contains(errMsg1), true);
        assertEquals(result.contains(errMsg2), true);
    }    
    
     /**
     * Test of getErrorsFromResponse method, of class OctopusApi.
     * Using many semi-random Octopus responses
     */
    @Test
    public void testGetErrorsFromResponseRandom() {
        for (int i = 0; i < 2000; i++) {
            List<String> errMsgs = new ArrayList<String>();
            String response = generateRandomErrorResponse(errMsgs);
            String result = OctopusApi.getErrorsFromResponse(response);
            for (String errMsg : errMsgs) {
                if (!result.contains(errMsg)) {
                    System.out.println(String.format("Could not find error msg: %s", errMsg));
                }
                assertEquals(result.contains(errMsg), true);
            }
        }
    } 
    
    /**
     * Generate a random error response
     * @param errorMsgList
     * @return 
     */
    private String generateRandomErrorResponse(List<String> errorMsgList) {
        final int maxNumMsgs = 15;
        String errMsg = "";
        String prefix = generateRandomString();
        String suffix = generateRandomString();
        
        int numDetails = (int)(rand.nextDouble() * maxNumMsgs);
        String errorFormatPrefix = "var errorData = {\"title\":\"Bad request\",\"message\":\"There was a problem with your request.\",\"details\":{\"ErrorMessage\":\"There was a problem with your request.\",\"Errors\":[";
        String errorFormatSuffix = "]}};";
        errMsg += prefix;
        errMsg += errorFormatPrefix;
        for (int i = 0; i < numDetails; i++) {
            String randomErrMsg = generateRandomString();
            errMsg += String.format("\"%s\"", randomErrMsg);
            errorMsgList.add(randomErrMsg);
            if (i < numDetails - 1)
                errMsg += ", ";
        }
        errMsg += errorFormatSuffix;
        errMsg += suffix;
        return errMsg;
    }
    
    private String generateRandomString() {
        final int maxCharacters = 500;
        int numCharacters = (int)(rand.nextDouble()*maxCharacters);
        
        final int minAscii = 32;
        final int maxAscii = 126;
        String msg = "";
        for (int i = 0; i < numCharacters; i++) {
            int randInt = (int)(rand.nextDouble()*(maxAscii - minAscii) + minAscii + 0.5);
            Character randChar = (char)randInt;
            //Do not allow forbidden characters
            if (forbiddenChars.contains(randChar))
                continue; //Just skip it
            
            msg += (char) randInt;
        }
        return msg;
    }
}
