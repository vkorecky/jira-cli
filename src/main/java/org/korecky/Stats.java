package org.korecky;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import org.korecky.configuration.Configuration;
import org.korecky.configuration.Team;
import org.korecky.dto.Issue;
import org.korecky.dto.Sprint;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Stats {
    public static final String EMPTY_EPIC = "Others";
    private final Configuration configuration;
    private final JiraClient jiraClient;

    public Stats(Configuration configuration) {
        this.configuration = configuration;
        this.jiraClient = new JiraClient(
                configuration.getServer().getUrl(),
                configuration.getServer().getUsername(),
                configuration.getServer().getPassword());
    }

    public void generate() throws JsonProcessingException {
        Sprint sprint = loadSprintDetail();
        List<Issue> sprintIssues = loadInformationFromJIRA();
        createEpicsReport(sprint, sprintIssues);
        createTeamVelocityReport(sprint, sprintIssues);
    }

    private List<Issue> loadInformationFromJIRA() throws JsonProcessingException {
        return jiraClient.getSprintIssues(configuration.getSprintId(), 100);
    }

    private Sprint loadSprintDetail() throws JsonProcessingException {
        return jiraClient.getSprintDetail(configuration.getSprintId());
    }

    private void createEpicsReport(Sprint sprint, List<Issue> sprintIssues) {
        Double storyPointsSum = 0.0;
        Map<String, Double> epics = new HashMap<>();
        for (Issue issue : sprintIssues) {
            String epicName = getEpicName(issue);
            Double storyPoints = getStoryPoints(issue);
            if (storyPoints > 0) {
                storyPointsSum += storyPoints;
                if (epics.containsKey(epicName)) {
                    Double oldStoryPoints = epics.get(epicName);
                    epics.put(epicName, oldStoryPoints + storyPoints);
                } else
                    epics.put(epicName, storyPoints);
            }
        }
        Path csvFilePath = Path.of(configuration.getOutputFolder(), sprint.getName() + "Epics.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath.toFile()))) {
            writer.writeNext(new String[]{"Epic name", "Story Points", "Percentage"});
            for (String epicName : epics.keySet()) {
                writer.writeNext(new String[]{epicName, String.valueOf(epics.get(epicName)), String.valueOf(epics.get(epicName) / storyPointsSum)});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTeamVelocityReport(Sprint sprint, List<Issue> sprintIssues) {
        Double storyPointsSum = 0.0;
        Map<String, Double> teams = getTeamsHashMap();
        Map<String, Double> members = getMembersHashMap();
        for (Issue issue : sprintIssues) {
            boolean isClosedInTheSprint = isClosedInTheSprint(issue, sprint);
            if (isClosedInTheSprint) {
                String assignee = getAssigne(issue);
                String team = getTeam(assignee);
                Double storyPoints = getStoryPoints(issue);
                if (storyPoints > 0) {
                    storyPointsSum += storyPoints;
                    if (teams.containsKey(team)) {
                        Double oldStoryPoints = teams.get(team);
                        teams.put(team, oldStoryPoints + storyPoints);
                    } else
                        teams.put(team, storyPoints);
                    if (members.containsKey(assignee)) {
                        Double oldStoryPoints = members.get(assignee);
                        members.put(assignee, oldStoryPoints + storyPoints);
                    } else
                        members.put(assignee, storyPoints);
                }
            }
        }
        Path csvTeamsFilePath = Path.of(configuration.getOutputFolder(), sprint.getName() + "VelocityOfTeams.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvTeamsFilePath.toFile()))) {
            List<String> headers = new ArrayList<>(List.of("Sprint", "From", "To", "Total"));
            headers.addAll(teams.keySet());
            writer.writeNext(headers.toArray(new String[0]));

            List<String> values = new ArrayList<>(List.of(sprint.getName(), sprint.getStartDate().toString(), sprint.getEndDate().toString(), storyPointsSum.toString()));
            for (String team : teams.keySet()) {
                values.add(teams.get(team).toString());
            }
            writer.writeNext(values.toArray(new String[0]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Path csvMembersFilePath = Path.of(configuration.getOutputFolder(), sprint.getName() + "VelocityOfMembers.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvMembersFilePath.toFile()))) {
            List<String> headers = new ArrayList<>(List.of("Sprint", "From", "To", "Total"));
            headers.addAll(members.keySet());
            writer.writeNext(headers.toArray(new String[0]));

            List<String> values = new ArrayList<>(List.of(sprint.getName(), sprint.getStartDate().toString(), sprint.getEndDate().toString(), storyPointsSum.toString()));
            for (String member : members.keySet()) {
                values.add(members.get(member).toString());
            }
            writer.writeNext(values.toArray(new String[0]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Double> getTeamsHashMap() {
        Map<String, Double> teams = new HashMap<>();
        for (Team team : configuration.getTeams()){
            teams.put(team.getName(), 0.0);
        }
        return teams;
    }

    private Map<String, Double> getMembersHashMap() {
        Map<String, Double> members = new HashMap<>();
        for (Team team : configuration.getTeams()){
            for (String member : team.getMembers()) {
                members.put(member, 0.0);
            }
        }
        return members;
    }

    private String getEpicName(Issue issue) {
        JsonNode fields = issue.getFields();
        JsonNode epic = fields.findValue("epic");
        if (epic == null)
            return EMPTY_EPIC;
        JsonNode epicKey = epic.findValue("key");
        JsonNode epicSummary = epic.findValue("summary");
        return epicKey.asText() + " - " + epicSummary.asText();
    }

    private String getAssigne(Issue issue) {
        JsonNode fields = issue.getFields();
        JsonNode assignee = fields.findValue("assignee");
        if (assignee == null)
            return null;
        JsonNode emailAddress = fields.findValue("emailAddress");
        return emailAddress.asText();
    }

    private String getTeam(String assigne) {
        for (Team team : configuration.getTeams()) {
            if (team.getMembers().contains(assigne))
                return team.getName();
        }
        return null;
    }

    private Double getStoryPoints(Issue issue) {
        JsonNode fields = issue.getFields();
        JsonNode storyPoints = fields.findValue(configuration.getStoryPointsColumn());
        return storyPoints.asDouble();
    }

    private boolean isDone(Issue issue) {
        JsonNode fields = issue.getFields();
        JsonNode status = fields.findValue("status");
        JsonNode statusCategory = status.findValue("statusCategory");
        JsonNode statusName = statusCategory.findValue("name");
        return "Done".equals(statusName.asText());
    }

    private boolean isClosedInTheSprint(Issue issue, Sprint sprint){
        boolean isDone = isDone(issue);
        if (!isDone)
            return false;

        JsonNode fields = issue.getFields();
        JsonNode currentSprint = fields.findValue("sprint");
        if (currentSprint == null){
            JsonNode closedSprints = fields.findValue("closedSprints");
            if ((closedSprints == null) || (closedSprints.size() <= 0))
                return false;

            for (int i=0; i<closedSprints.size(); i++){
                int closedSprintId = closedSprints.get(i).findValue("id").asInt();
                if (closedSprintId == sprint.getId())
                    return true;
            }
            return false;
        }
        int currentSprintId = currentSprint.findValue("id").asInt();
        return sprint.getId() == currentSprintId;
    }
}
