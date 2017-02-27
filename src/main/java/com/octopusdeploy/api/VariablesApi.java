package com.octopusdeploy.api;

import com.octopusdeploy.api.data.Variable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;

public class VariablesApi {
    private final AuthenticatedWebClient webClient;

    public VariablesApi(AuthenticatedWebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Get the variables for a combination of release and environment, return null otherwise.
     * @param releaseId The id of the Release.
     * @param environmentId The id of the Environment.
     * @param entryProperties entry properties
     * @return A set of all variables for a given Release and Environment combination.
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Set<Variable> getVariablesByReleaseAndEnvironment(String releaseId, String environmentId, Properties entryProperties) throws IllegalArgumentException, IOException {
        Set<Variable> variables = new HashSet<Variable>();

        AuthenticatedWebClient.WebResponse response = webClient.get("api/releases/" + releaseId + "/deployments/preview/" + environmentId);
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        JSONObject form = json.getJSONObject("Form");
        if (form != null){
            JSONObject formValues = form.getJSONObject("Values");
            for (Object obj : form.getJSONArray("Elements")) {
                JSONObject jsonObj = (JSONObject) obj;
                String id = jsonObj.getString("Name");
                String name = jsonObj.getJSONObject("Control").getString("Name");
                String value = formValues.getString(id);

                String entryValue = entryProperties.getProperty(name);
                if (StringUtils.isNotEmpty(entryValue)) {
                    value = entryValue;
                }
                String description = jsonObj.getJSONObject("Control").getString("Description");
                variables.add(new Variable(id, name, value, description));
            }
        }

        return variables;
    }
}
