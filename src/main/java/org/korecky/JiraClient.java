package org.korecky;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;


public class JiraClient {

    private final String jiraUrl;
    private final String username;
    private final String password;
    private CloseableHttpClient httpClient;


    public JiraClient(String jiraUrl, String username, String password) {
        this.jiraUrl = jiraUrl;
        this.username = username;
        this.password = password;
        this.httpClient = HttpClients.createDefault();
    }

    public SprintDetail getSprintDetails(String sprintId) throws JsonProcessingException {
        URI apiUrl = URI.create(jiraUrl).resolve("/rest/agile/1.0/sprint/" + sprintId);
        String jsonString = getResponse(apiUrl);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        SprintDetail sprintDetail = objectMapper.readValue(jsonString, SprintDetail.class);

        return sprintDetail;
    }

    public String getIssueDetails(String issueKey) {
        URI apiUrl = URI.create(jiraUrl).resolve("/rest/api/2/issue/" + issueKey);
        return getResponse(apiUrl);
    }

    private String getResponse(URI apiUrl) {
        StringBuilder responseBodyBuilder = new StringBuilder();
        try {
            // Create HTTP GET request
            HttpGet getRequest = new HttpGet(apiUrl);

            // Set authentication credentials
            getRequest.setHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));

            try {
                // Execute the request
                HttpResponse response = httpClient.execute(getRequest);

                // Check for a successful response (HTTP 200)
                if (response.getStatusLine().getStatusCode() == 200) {
                    // Read and print the response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBodyBuilder.append(line);
                    }
                } else {
                    throw new RuntimeException(response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return responseBodyBuilder.toString();
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
}
