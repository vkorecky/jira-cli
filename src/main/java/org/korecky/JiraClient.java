package org.korecky;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Paths;

public class JiraClient {

    private final String jiraUrl;
    private final String username;
    private final String password;

    public JiraClient(String jiraUrl, String username, String password) {
        this.jiraUrl = jiraUrl;
        this.username = username;
        this.password = password;
    }

    public String getIssueDetails(String issueKey) {
        URI apiUrl =  Paths.get(jiraUrl, "/rest/api/2/issue/" , issueKey).toUri();

        // Create HttpClient
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
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
                        System.out.println(line);
                    }
                } else {
                    throw new RuntimeException(response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return "Issue details for " + issueKey;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
