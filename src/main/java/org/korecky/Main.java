package org.korecky;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        // Replace these with your JIRA server details and credentials
        String jiraServerUrl = "https://your-jira-server.com";
        String username = "your-username";
        String password = "your-password";

        // JIRA REST API endpoint for retrieving issue details (replace 'KEY' with the actual issue key)
        String issueKey = "ABC-12345";
        String apiUrl = jiraServerUrl + "/rest/api/2/issue/" + issueKey;

        // Create HttpClient
        HttpClient httpClient = HttpClients.createDefault();

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
                System.err.println("Error: " + response.getStatusLine().getReasonPhrase());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close the HttpClient
            httpClient.getConnectionManager().shutdown();
        }
    }
}