package com.octopusdeploy.api;

import com.octopusdeploy.api.data.Tenant;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Methods for the Tenants aspects of the Octopus API
 */
public class TenantsApi {
    private final AuthenticatedWebClient webClient;

    public TenantsApi(AuthenticatedWebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Uses the authenticated web client to pull all tenants from the api and
     * convert them to POJOs
     * @return a Set of Tenants (may be empty)
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Set<Tenant> getAllTenants() throws IllegalArgumentException, IOException {
        HashSet<Tenant> tenants = new HashSet<Tenant>();
        AuthenticatedWebClient.WebResponse response = webClient.get("api/tenants/all");
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONArray json = (JSONArray)JSONSerializer.toJSON(response.getContent());
        for (Object obj : json) {
            JSONObject jsonObj = (JSONObject)obj;
            String id = jsonObj.getString("Id");
            String name = jsonObj.getString("Name");
            tenants.add(new Tenant(id, name));
        }
        return tenants;
    }

    /**
     * Get the Tenant with the given name if it exists, return null otherwise.
     * Only selects the tenant if the name is an exact match (including case)
     * @param name The name of the Tenant to find.
     * @return The Tenant with that name.
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Tenant getTenantByName(String name) throws IllegalArgumentException, IOException {
        return getTenantByName(name, false);
    }

    /**
     * Get the Tenant with the given name if it exists, return null otherwise.
     * @param name The name of the Tenant to find.
     * @param ignoreCase when true uses equalsIgnoreCase in the name check
     * @return The Environment with that name.
     * @throws IllegalArgumentException when the web client receives a bad parameter
     * @throws IOException When the AuthenticatedWebClient receives and error response code
     */
    public Tenant getTenantByName(String name, boolean ignoreCase) throws IllegalArgumentException, IOException {
        Set<Tenant> tenants = getAllTenants();
        for (Tenant tenant : tenants) {
            if ((ignoreCase && name.equalsIgnoreCase(tenant.getName())) ||
               (!ignoreCase && name.equals(tenant.getName()))) {
                return tenant;
            }
        }
        return null;
    }
}
