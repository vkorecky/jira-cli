package org.korecky.jiracli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.korecky.jiracli.configuration.Configuration;
import org.korecky.jiracli.configuration.Team;
import org.korecky.jiracli.dto.Component;
import org.korecky.jiracli.dto.Issue;
import org.korecky.jiracli.dto.Sprint;
import org.korecky.jiracli.report.Work;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class Reports {
    public static final String EMPTY_EPIC = "Others";
    private final Configuration configuration;
    private final JiraClient jiraClient;

    public Reports(Configuration configuration) {
        this.configuration = configuration;
        this.jiraClient = new JiraClient(
                configuration.getServer().getUrl(),
                configuration.getServer().getUsername(),
                configuration.getServer().getPassword());
    }

    public void generate() throws IOException {
        Sprint sprint = loadSprintDetail();
        List<Issue> sprintIssues = loadInformationFromJIRA();

        XSSFWorkbook workbook = new XSSFWorkbook();
        createSpreadsheetEpics(workbook, sprint, sprintIssues);
        createSpreadsheetVelocityOfTeams(workbook, sprint, sprintIssues);

        Path excelFilePath = Path.of(configuration.getOutputFolder(), sprint.getName() + ".xlsx");
        try (FileOutputStream out = new FileOutputStream(excelFilePath.toAbsolutePath().toString())) {
            workbook.write(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Issue> loadInformationFromJIRA() throws JsonProcessingException {
        return jiraClient.getSprintIssues(configuration.getSprintId(), 100);
    }

    private Sprint loadSprintDetail() throws JsonProcessingException {
        return jiraClient.getSprintDetail(configuration.getSprintId());
    }

    private void createSpreadsheetEpics(XSSFWorkbook workbook, Sprint sprint, List<Issue> sprintIssues) {
        Double plannedStoryPointsSum = 0.0;
        Double deliveredStoryPointsSum = 0.0;
        Map<String, Double> epicsPlanned = new HashMap<>();
        Map<String, Double> epicsDelivered = new HashMap<>();
        Map<String, List<String>> issues = new HashMap<>();
        Map<String, String> epicLinks = new HashMap<>();
        for (Issue issue : sprintIssues) {
            String epicName = getEpicName(issue);
            String epicLink = getEpicLink(issue);
            Double storyPoints = getStoryPoints(issue);
            String issueLink = getIssueLink(issue);

            epicLinks.put(epicName,epicLink);

            List<String> existingIssues = new ArrayList<>();
            if (issues.containsKey(epicName))
                existingIssues = issues.get(epicName);
            existingIssues.add(issueLink);
            issues.put(epicName,existingIssues);

            if (storyPoints > 0) {
                plannedStoryPointsSum += storyPoints;
                boolean isClosedInTheSprint = isClosedInTheSprint(issue, sprint);
                if (isClosedInTheSprint) {
                    deliveredStoryPointsSum += storyPoints;
                    if (epicsDelivered.containsKey(epicName)) {
                        Double oldStoryPoints = epicsDelivered.get(epicName);
                        epicsDelivered.put(epicName, oldStoryPoints + storyPoints);
                    } else
                        epicsDelivered.put(epicName, storyPoints);
                } else {
                    if (epicsPlanned.containsKey(epicName)) {
                        Double oldStoryPoints = epicsPlanned.get(epicName);
                        epicsPlanned.put(epicName, oldStoryPoints + storyPoints);
                    } else
                        epicsPlanned.put(epicName, storyPoints);
                }
            }
        }

        XSSFSheet spreadsheetEpicsPlanned = workbook.createSheet(" Epics planned");
        XSSFRow row = spreadsheetEpicsPlanned.createRow(0);
        row.createCell(0).setCellValue("Epic name");
        row.createCell(1).setCellValue("Story Points");
        row.createCell(2).setCellValue("Percentage");

        int rowIndex = 1;
        for (String epicName : epicsPlanned.keySet()) {
            row = spreadsheetEpicsPlanned.createRow(rowIndex);
            row.createCell(0).setCellValue(epicName);
            row.createCell(1).setCellValue(epicsPlanned.get(epicName));
            row.createCell(2).setCellValue(epicsPlanned.get(epicName) / plannedStoryPointsSum);
            rowIndex++;
        }

        XSSFSheet spreadsheetEpicsDelivered = workbook.createSheet(" Epics delivered");
        row = spreadsheetEpicsDelivered.createRow(0);
        row.createCell(0).setCellValue("Epic name");
        row.createCell(1).setCellValue("Story Points");
        row.createCell(2).setCellValue("Percentage");

        rowIndex = 1;
        for (String epicName : epicsPlanned.keySet()) {
            row = spreadsheetEpicsDelivered.createRow(rowIndex);
            row.createCell(0).setCellValue(epicName);
            row.createCell(1).setCellValue(epicsPlanned.get(epicName));
            row.createCell(2).setCellValue(epicsPlanned.get(epicName) / deliveredStoryPointsSum);
            rowIndex++;
        }

    }


    private void createSpreadsheetVelocityOfTeams(XSSFWorkbook workbook, Sprint sprint, List<Issue> sprintIssues) throws IOException {
        double plannedStoryPointsSum = 0.0;
        double deliveredStoryPointsSum = 0.0;
        Map<String, Work> teams = new HashMap<>();
        for (Issue issue : sprintIssues) {
            String team = getTeam(issue);
            Work work = new Work();
            if (teams.containsKey(team))
                work = teams.get(team);
            Double storyPoints = getStoryPoints(issue);
            if (storyPoints > 0) {
                plannedStoryPointsSum += storyPoints;
                boolean isClosedInTheSprint = isClosedInTheSprint(issue, sprint);
                work.setPlanned(work.getPlanned() + storyPoints);
                if (isClosedInTheSprint) {
                    deliveredStoryPointsSum += storyPoints;
                    work.setFinished(work.getFinished() + storyPoints);
                }
                teams.put(team, work);
            }
        }

        XSSFSheet spreadsheet = workbook.createSheet("VelocityOfTeams");
        XSSFRow rowHeader = spreadsheet.createRow(0);
        rowHeader.createCell(0).setCellValue("Sprint");
        rowHeader.createCell(1).setCellValue("From");
        rowHeader.createCell(2).setCellValue("To");
        rowHeader.createCell(3).setCellValue("Total");

        XSSFRow rowPlannedWork= spreadsheet.createRow(1);
        rowPlannedWork.createCell(0).setCellValue(sprint.getName());
        rowPlannedWork.createCell(1).setCellValue(sprint.getStartDate().toString());
        rowPlannedWork.createCell(2).setCellValue(sprint.getEndDate().toString());
        rowPlannedWork.createCell(3).setCellValue(plannedStoryPointsSum);

        XSSFRow rowFinishedWork= spreadsheet.createRow(2);
        rowFinishedWork.createCell(0).setCellValue(sprint.getName());
        rowFinishedWork.createCell(1).setCellValue(sprint.getStartDate().toString());
        rowFinishedWork.createCell(2).setCellValue(sprint.getEndDate().toString());
        rowFinishedWork.createCell(3).setCellValue(deliveredStoryPointsSum);

        int columnIndex = 4;
        for (String teamName : teams.keySet()) {
            rowHeader.createCell(columnIndex).setCellValue(teamName);
            rowPlannedWork.createCell(columnIndex).setCellValue(teams.get(teamName).getPlanned());
            rowFinishedWork.createCell(columnIndex).setCellValue(teams.get(teamName).getFinished());
            columnIndex++;
        }
    }

    private String getEpicName(Issue issue) {
        JsonNode fields = issue.getFields();
        JsonNode epic = fields.findValue("epic");
        if ((epic == null) || (epic.isEmpty()))
            return EMPTY_EPIC;
        JsonNode epicSummary = epic.findValue("summary");
        return epicSummary.asText();
    }

    private String getEpicLink(Issue issue) {
        JsonNode fields = issue.getFields();
        JsonNode epic = fields.findValue("epic");
        if (epic == null)
            return EMPTY_EPIC;
        JsonNode epicKey = epic.findValue("key");
        return configuration.getServer().getUrl() + "/browse/" + epicKey;
    }

    private String getIssueLink(Issue issue) {
        String issueKey = issue.getKey();
        return configuration.getServer().getUrl() + "/browse/" + issueKey;
    }

    private String getTeam(Issue issue) {
        if (issue.getAssignee() != null) {
            for (Team team : configuration.getTeams()) {
                if (team.getMembers().contains(issue.getAssignee().getEmailAddress()))
                    return team.getName();
            }
        } else {
            for (Component component : issue.getComponents()) {
                for (Team team : configuration.getTeams()) {
                    if (team.getComponents().contains(component.getName()))
                        return team.getName();
                }
            }
        }
        return "Unknown";
    }

    private Double getStoryPoints(Issue issue) {
        JsonNode fields = issue.getFields();
        JsonNode storyPoints = fields.findValue(configuration.getStoryPointsColumn());
        if (storyPoints == null)
            return 0.0;
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
        if ((currentSprint == null) || (currentSprint.findValue("id") == null)) {
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
