package com.octopusdeploy.api;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Parses errors from Octopus html/javascript responses
 * @author jlabroad
 */
public class ErrorParser {
    
    /** Find a group of messages in the format: "Errors":["error message 1", "error message 2", "error message 3"] */    
    protected static final String errDetailsOutsideString = "(?:\\\"Errors\\\")(?:[^\\[]\\[)(?<fullDetailString>[^\\]]+)";
    protected static Pattern errDetailsOutsidePattern = Pattern.compile(errDetailsOutsideString);

    /** Parse each individual message from "error message 1", "error message 2", "error message 3" */
    protected static final String errDetailsInsideString = "(?:\\\")(?<singleError>[^\\\"]+)*(?:\\\")";
    protected static Pattern errDetailsInsidePattern = Pattern.compile(errDetailsInsideString);
    
    /**
     * Parse any errors from the returned HTML/javascript from Octopus
     * @param response The Octopus html response that may include error data
     * @return A list of error strings
     */
    public static String getErrorsFromResponse(String response) {
        List<String> errorStrings = new ArrayList<String>();        

        //Get the error title and main message
        String errorTitle = getErrorDataByFieldName("title", response);
        if (!errorTitle.isEmpty()) {
            errorStrings.add(String.format("%s", errorTitle));
        }
     
        //Get the error details
        String errorDetailMessage = getErrorDataByFieldName("ErrorMessage", response);
        if (!errorDetailMessage.isEmpty()) {
            errorStrings.add("\t" + errorDetailMessage);        
        }
        errorStrings.addAll(getErrorDetails(response));       

        String errorMsg = "";
        for (String err : errorStrings) {
            errorMsg += String.format("%s\n", err);
        }          
        
        return errorMsg;
    }
    
    /**
     * Grabs a single error data field from an Octopus html response
     * @param fieldName The field name of the error string
     * @param response The field data
     * @return The error data
     */
    protected static String getErrorDataByFieldName(String fieldName, String response) {
        //Get the next string in script parameter list: "var errorData = {<fieldName>:"Field value", ...
        final String patternString = String.format("(?:errorData.+)(?:\"%s\")(?:[:\\[\"]+)(?<fieldValue>[^\"]+)", fieldName);
        
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(response);
        String errData = "";
        if (matcher.find() && matcher.groupCount() > 0) {
            errData = matcher.group("fieldValue");
        }
        return errData;
    }
    
    /**
     * Returns a list of "Errors" values from Octopus html response
     * @param response The full Octopus html response
     * @return a list of error details
     */
    protected static List<String> getErrorDetails(String response) {
        List<String> errorList = new ArrayList<String>();

        Matcher m = errDetailsOutsidePattern.matcher(response);
        if (m.find() && m.groupCount() > 0) {
            //Split up the list of error messages into individual messages
            String errors = m.group("fullDetailString");
            m = errDetailsInsidePattern.matcher(errors);
            while (m.find() && m.groupCount() > 0) {
                String singleError = StringEscapeUtils.unescapeJava(m.group("singleError"));
                errorList.add("\t" + singleError);
            }
        }    
        return errorList;
    }     
}
