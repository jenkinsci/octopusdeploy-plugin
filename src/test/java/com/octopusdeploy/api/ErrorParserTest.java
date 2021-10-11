package com.octopusdeploy.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class ErrorParserTest {

    private Random rand = null;
    private final Set<Character> forbiddenChars = new HashSet<>(Arrays.asList('\"', '\\', ']'));

    @BeforeEach
    public void setUp() {
        rand = new Random();
    }

    @Test
    public void testGetErrorsFromResponseStatic() {
        System.out.println("getErrorsFromResponse");
        String errMsg1 = "No package version was specified for the step 'test nuget'";
        String errMsg2 = "Error msg number 2";
        String response = String.format("<!DOCTYPE html><html lang=\"en\"><head>  <title ng-bind=\"$root.pageTitle + ' - Octopus Deploy'\">Octopus Deploy</title>    <link rel=\"stylesheet\" href=\"/css/octopus.min.css\" />    <link rel=\"apple-touch-icon\" href=\"/img/icons/Octopus-96x96.png\" />  <link rel=\"icon\" href=\"/img/icons/Octopus-96x96.png\" />  <!--[if IE]><link rel=\"shortcut icon\" href=\"img/icons/Octopus-16x16.ico\"><![endif]-->  <meta name=\"msapplication-TileColor\" content=\"#2F93E0\">  <meta name=\"msapplication-TileImage\" content=\"/img/icons/Octopus-144x144-Transparent.png\">  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">  <meta name=\"viewport\" content=\"width=device-width\">        <script type=\"text/javascript\" src=\"/octopus.min.js\"></script>        <script>    $(function() {        var errorData = {\"title\":\"Bad request\",\"message\":\"There was a problem with your request.\",\"details\":{\"ErrorMessage\":\"There was a problem with your request.\",\"Errors\":[\"%s\", \"%s\"]}};        $(\"#err-title\").text(errorData.title);        $(\"#err-message\").text(errorData.message);        $(\"#err-details\").text(angular.toJson(errorData.details, true));        $(\"#show-details\").click(function() {            $(\"#show-details\").hide();            $(\"#err-details\").show();         });        $(\"#connection-failed\").show();    });    </script></head><body><div id='initial-page-loader'>  <div class='container'>    <div class='narrow'>      <div class='box-shadow'>        <div class='pad30'>          <div class='connection-failed'>            <div class=\"clippy\">                <img src=\"/img/layout/octoclippy.png\" width=\"64\" height=\"64\" alt=\"Octoclippy is here to help!\" title=\"Octoclippy is here to help!\" />                <div class=\"clippy-says\">                    <h2 id='err-title'>Oops!</h2>                    <p id='err-message'>Something went wrong...</p>                    <a class='btn btn-info' id='show-details'>Show details</a>                    <pre id='err-details' style='display: none'></pre>                </div>            </div>          </div>        </div>      </div>    </div>  </div></div></body></html>",
                errMsg1, errMsg2);
        String result = ErrorParser.getErrorsFromResponse(response);

        assertThat(result).contains(errMsg1, errMsg2);
        Assertions.assertTrue(result.contains(errMsg1));
        Assertions.assertTrue(result.contains(errMsg2));
    }

    @Test
    public void testGetErrorsFromResponseRandom() {
        for (int i = 0; i < 2000; i++) {
            List<String> errMsgs = new ArrayList<>();
            String response = generateRandomErrorResponse(errMsgs);
            String result = ErrorParser.getErrorsFromResponse(response);

            errMsgs.forEach(msg -> assertThat(result).contains(msg));
        }
    }

    private String generateRandomErrorResponse(List<String> errorMsgList) {
        final int maxNumMsgs = 15;
        StringBuilder errMsg = new StringBuilder();
        String prefix = generateRandomString();
        String suffix = generateRandomString();

        int numDetails = (int) (new Random().nextDouble() * maxNumMsgs);
        String errorFormatPrefix = "var errorData = {\"title\":\"Bad request\",\"message\":\"There was a problem with your request.\",\"details\":{\"ErrorMessage\":\"There was a problem with your request.\",\"Errors\":[";
        String errorFormatSuffix = "]}};";
        errMsg.append(prefix);
        errMsg.append(errorFormatPrefix);
        for (int i = 0; i < numDetails; i++) {
            String randomErrMsg = generateRandomString();
            errMsg.append(String.format("\"%s\"", randomErrMsg));
            errorMsgList.add(randomErrMsg);
            if (i < numDetails - 1)
                errMsg.append(", ");
        }
        errMsg.append(errorFormatSuffix);
        errMsg.append(suffix);
        return errMsg.toString();
    }

    private String generateRandomString() {
        final int maxCharacters = 500;
        int numCharacters = (int) (rand.nextDouble() * maxCharacters);

        final int minAscii = 32;
        final int maxAscii = 126;
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < numCharacters; i++) {
            int randInt = (int) (rand.nextDouble() * (maxAscii - minAscii) + minAscii + 0.5);
            Character randChar = (char) randInt;
            //Do not allow forbidden characters
            if (forbiddenChars.contains(randChar))
                continue; //Just skip it

            msg.append((char) randInt);
        }
        return msg.toString();
    }
}
