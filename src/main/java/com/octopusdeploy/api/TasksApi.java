package com.octopusdeploy.api;

import com.octopusdeploy.api.data.Task;
import java.io.IOException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class TasksApi {
    private final AuthenticatedWebClient webClient;

    public TasksApi(AuthenticatedWebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Retrieves a task by its id.
     * @param taskId task id
     * @return a Task object
     * @throws IllegalArgumentException  when the web client receives a bad parameter
     * @throws IOException  When the AuthenticatedWebClient receives and error response code
     */
    public Task getTask(String taskId) throws IllegalArgumentException, IOException {
        AuthenticatedWebClient.WebResponse response = webClient.get("tasks/" + taskId);
        if (response.isErrorCode()) {
            throw new IOException(String.format("Code %s - %n%s", response.getCode(), response.getContent()));
        }
        JSONObject json = (JSONObject)JSONSerializer.toJSON(response.getContent());
        String id = json.getString("Id");
        String name = json.getString("Name");
        String description = json.getString("Description");
        String state = json.getString("State");
        boolean isCompleted = json.getBoolean("IsCompleted");
        return new Task(id, name, description, state, isCompleted);
    }
}
