package org.korecky;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.korecky.dto.Issue;
import org.korecky.dto.Sprint;
import org.korecky.dto.SprintIssues;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class JiraClient {

    private final String jiraUrl;
    private final String username;
    private final String password;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;


    public JiraClient(String jiraUrl, String username, String password) {
        this.jiraUrl = jiraUrl;
        this.username = username;
        this.password = password;
        this.httpClient = HttpClients.createDefault();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Sprint getSprintDetail(String sprintId) throws JsonProcessingException {
        URI apiUrl = URI.create(jiraUrl).resolve("/rest/agile/1.0/sprint/" + sprintId);
        String jsonString = getResponse(apiUrl);
        return objectMapper.readValue(jsonString, Sprint.class);
    }

    public List<Issue> getSprintIssues(String sprintId, int maxResultsPerPage) throws JsonProcessingException {
        boolean nextPage = true;
        List<Issue> issues = new ArrayList<>();
        while (nextPage) {
            URI apiUrl = URI.create(jiraUrl).resolve("/rest/agile/1.0/sprint/" + sprintId + "/issue?startAt=" + issues.size() + "&maxResults=" + maxResultsPerPage);
            String jsonString = getResponse(apiUrl);
            SprintIssues sprintIssues = objectMapper.readValue(jsonString, SprintIssues.class);
            issues.addAll(sprintIssues.getIssues());
            if (sprintIssues.getTotal() <= issues.size())
                nextPage = false;
        }
        return issues;
    }

    public Issue getIssueDetail(String issueKey) throws JsonProcessingException {
        URI apiUrl = URI.create(jiraUrl).resolve("/rest/api/2/issue/" + issueKey);
        String jsonString = getResponse(apiUrl);
        return objectMapper.readValue(jsonString, Issue.class);
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
