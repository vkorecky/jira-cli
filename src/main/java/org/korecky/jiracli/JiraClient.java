package org.korecky.jiracli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.poi.util.StringUtil;
import org.korecky.jiracli.dto.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.stream.Collectors;


public class JiraClient {
    private static final Logger LOGGER = LogManager.getLogger(JiraClient.class);

    private final String jiraUrl;
    private final String username;
    private final String password;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;


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

    public List<Sprint> findSprintsByName(String keyword) throws IOException {
        List<Sprint> matchingSprints = new ArrayList<>();
        List<Board> allBoards = getAllBoards();

        for (Board board : allBoards) {
            System.out.print("Searching board;" + board.getName() + " ... ");
            boolean isLast = false;
            int startAt = 0;
            while (!isLast) {
                URI sprintsApiUrl = URI.create(jiraUrl).resolve("/rest/agile/1.0/board/" + board.getId() + "/sprint?startAt=" + startAt);
                String jsonString = getResponse(sprintsApiUrl);
                if (StringUtil.isNotBlank(jsonString)) {
                    SprintList sprintList = objectMapper.readValue(jsonString, SprintList.class);

                    // Filter sprints by name and add them to the result list
                    List<Sprint> filtered = new ArrayList<>();
                    for (Sprint sprint : sprintList.getValues()) {
                        if (sprint.getName().toLowerCase().contains(keyword.toLowerCase())) {
                            filtered.add(sprint);
                        }
                    }
                    matchingSprints.addAll(filtered);

                    isLast = sprintList.isLast();
                    startAt += sprintList.getValues().size();
                } else {
                    isLast = true;
                }
            }
            System.out.println("Done");
        }
        return matchingSprints;
    }

    private List<Board> getAllBoards() throws IOException {
        List<Board> allBoards = new ArrayList<>();
        boolean isLast = false;
        int startAt = 0;
        while (!isLast) {
            URI boardsApiUrl = URI.create(jiraUrl).resolve("/rest/agile/1.0/board?startAt=" + startAt);
            String jsonString = getResponse(boardsApiUrl);
            if (StringUtil.isNotBlank(jsonString)) {
                BoardList boardList = objectMapper.readValue(jsonString, BoardList.class);
                allBoards.addAll(boardList.getValues());
                isLast = boardList.isLast();
                startAt += boardList.getValues().size();
            } else {
                isLast = true;
            }
        }
        return allBoards;
    }


    public Sprint getSprintDetail(int sprintId) throws JsonProcessingException {
        URI apiUrl = URI.create(jiraUrl).resolve("/rest/agile/1.0/sprint/" + sprintId);
        String jsonString = getResponse(apiUrl);
        return objectMapper.readValue(jsonString, Sprint.class);
    }

    public List<Issue> getSprintIssues(int sprintId, int maxResultsPerPage) throws JsonProcessingException {
        boolean nextPage = true;
        List<Issue> issues = new ArrayList<>();
        int startAt = 0;
        while (nextPage) {
            URI apiUrl = URI.create(jiraUrl).resolve("/rest/agile/1.0/sprint/" + sprintId + "/issue?startAt=" + startAt + "&maxResults=" + maxResultsPerPage);
            String jsonString = getResponse(apiUrl);
            if (StringUtil.isNotBlank(jsonString)) {
                SprintIssues sprintIssues = objectMapper.readValue(jsonString, SprintIssues.class);
                for (Issue issue : sprintIssues.getIssues()) {
                    Assignee assignee = objectMapper.readValue(String.valueOf(issue.getFields().findValue("assignee")), Assignee.class);
                    issue.setAssignee(assignee);
                    List<Component> components = objectMapper.readerForListOf(Component.class).readValue(String.valueOf(issue.getFields().findValue("components")));
                    issue.setComponents(components);
                    List<String> labels = objectMapper.readerForListOf(String.class).readValue(String.valueOf(issue.getFields().findValue("labels")));
                    issue.setLabels(labels);
                    issues.add(issue);
                }
                startAt = issues.size() - 1;
                if (sprintIssues.getTotal() <= issues.size())
                    nextPage = false;
            }
            else {
                nextPage = false;
            }
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
            getRequest.setHeader("Accept", "application/json");
            getRequest.setHeader("Content-Type", "application/json");

            // Set authentication credentials
            getRequest.setHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));


            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                if (response.getStatusLine().getStatusCode() == 200) {// Read and print the response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBodyBuilder.append(line);
                    }
                } else {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                        String errorLine;
                        StringBuilder errorResponseBody = new StringBuilder();
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponseBody.append(errorLine);
                        }
                        LOGGER.warn("HTTP Error: 400 Bad Request. Response body: {}", errorResponseBody.toString());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to fetch data from " + apiUrl + ". Status: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error during HTTP request to " + apiUrl, e);
        }
        return responseBodyBuilder.toString();
    }
}
