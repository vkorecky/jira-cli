package org.korecky;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) {
        Options options = new Options()
                .addOption("url", true, "JIRA server URL")
                .addOption("username", true, "JIRA username")
                .addOption("password", true, "JIRA password")
                .addOption("issue", true, "JIRA issue key")
                .addOption("help", false, "Print usage information");

        try {
            // Parse command-line arguments
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            // Check if the "help" option is present
            if (cmd.hasOption("help") || args.length < 3) {
                printHelp(options);
                return;
            }

            // Get JIRA connection details
            String jiraUrl = cmd.getOptionValue("url");
            String username = cmd.getOptionValue("username");
            String password = cmd.getOptionValue("password");

            // Get the issue key (for demonstration purposes)
            String issueKey = cmd.getOptionValue("issue");

            // Perform some operation with JIRA, e.g., retrieve issue details
            JiraClient jiraClient = new JiraClient(jiraUrl, username, password);
            String issueDetails = jiraClient.getIssueDetails(issueKey);

            System.out.println("Issue Details:");
            System.out.println(issueDetails);

        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
            printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("JiraCommandLineApp", options);
    }
}