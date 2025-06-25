package org.korecky.jiracli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.korecky.jiracli.configuration.Configuration;
import org.korecky.jiracli.dto.Sprint;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Configuration configuration = loadConfig();

        // Fined all sprints containing NEO
        JiraClient jiraClient = new JiraClient(
                configuration.getServer().getUrl(),
                configuration.getServer().getUsername(),
                configuration.getServer().getPassword()
        );
        String keywordToSearch = "NEO";
        System.out.println("Searching for sprints with keyword: " + keywordToSearch);
        List<Sprint> foundSprints = jiraClient.findSprintsByName(keywordToSearch);

        if (foundSprints.isEmpty()) {
            System.out.println("No sprints found.");
        } else {
            System.out.println("Found sprints:");
            foundSprints.forEach(sprint ->
                    System.out.printf(" - ID: %d, Name: %s, State: %s%n",
                            sprint.getId(), sprint.getName(), sprint.getState())
            );
        }


        Reports reports = new Reports(configuration);
        reports.generate();
    }

    private static Configuration loadConfig() throws IOException {
        String homeFolder = System.getProperty("user.home");
        String configFilePath = homeFolder + File.separator + ".jira-cli" + File.separator + "config.yml";

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(new File(configFilePath), Configuration.class);
    }
}