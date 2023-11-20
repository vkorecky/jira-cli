package org.korecky;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.cli.*;
import org.korecky.dto.Sprint;

public class Main {
    public static void main(String[] args) {
        Options options = new Options()
                .addOption("help", false, "Print usage information")
                .addOption("jiraUrl", true, "JIRA server URL")
                .addOption("username", true, "JIRA username")
                .addOption("password", true, "JIRA password")
                .addOption("sprintId", true, "JIRA Sprint ID");

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help") || args.length < 3) {
                printHelp(options);
                return;
            }

            String jiraUrl = cmd.getOptionValue("jiraUrl");
            String username = cmd.getOptionValue("username");
            String password = cmd.getOptionValue("password");
            String sprintId = cmd.getOptionValue("sprintId");

            JiraClient jiraClient = new JiraClient(jiraUrl, username, password);
            Sprint sprint = jiraClient.getSprintDetails(sprintId);

            System.out.println("Sprint Details:");
            System.out.println(sprint);

        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
            printHelp(options);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("JiraCommandLineApp", options);
    }
}