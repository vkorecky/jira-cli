package org.korecky;

import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.korecky.dto.Issue;
import org.korecky.dto.Sprint;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.Mockito.when;

public class JiraClientTest {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static JiraClient jiraClient;
    AutoCloseable openMocks;
    @Mock
    private CloseableHttpClient httpClientMock;

    @BeforeClass
    public void setUp() throws Exception {
        openMocks = MockitoAnnotations.openMocks(this);
        jiraClient = new JiraClient("https://your-jira-server.com", "your-username", "your-password");

        Field httpClientField = JiraClient.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(jiraClient, httpClientMock);
    }

    @AfterClass
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test(dataProvider = "getSprintDetailDataProvider")
    public void getSprintDetail(String sprintId, Sprint expectedSprint, String responseJson) throws Exception {
        String url = "https://your-jira-server.com/rest/agile/1.0/sprint/" + sprintId;
        mockResponse(url, responseJson);

        Sprint sprint = jiraClient.getSprintDetail(sprintId);
        Assert.assertEquals(sprint, expectedSprint);
    }

    @Test(dataProvider = "getSprintIssuesDataProvider")
    public void getSprintIssues(String sprintId, List<Issue> expectedIssues, Integer maxResults, String firstResponseJson, String secondResponseJson) throws Exception {
        String url1 = "https://your-jira-server.com/rest/agile/1.0/sprint/" + sprintId + "/issue?startAt=0&maxResults=" + maxResults;
        mockResponse(url1, firstResponseJson);
        String url2 = "https://your-jira-server.com/rest/agile/1.0/sprint/" + sprintId + "/issue?startAt=1&maxResults=" + maxResults;
        mockResponse(url2, secondResponseJson);

        List<Issue> issues = jiraClient.getSprintIssues(sprintId, maxResults);
        for (int i=0; i<issues.size(); i++){
            Assert.assertEquals(issues.get(i).getId(), expectedIssues.get(i).getId());
            Assert.assertEquals(issues.get(i).getKey(), expectedIssues.get(i).getKey());
            Assert.assertEquals(issues.get(i).getSelf(), expectedIssues.get(i).getSelf());
            Assert.assertNotNull(issues.get(i).getFields());
        }
    }

    @Test(dataProvider = "getIssueDetailDataProvider")
    public void getIssueDetail(String issueKey, Issue expectedIssue, Integer expectedNumOfFields, String responseJson) throws Exception {
        String url = "https://your-jira-server.com/rest/api/2/issue/" + issueKey;
        mockResponse(url, responseJson);

        Issue issue = jiraClient.getIssueDetail(issueKey);
        Assert.assertEquals(issue.getId(), expectedIssue.getId());
        Assert.assertEquals(issue.getKey(), expectedIssue.getKey());
        Assert.assertEquals(issue.getSelf(), expectedIssue.getSelf());
        Assert.assertEquals(issue.getFields().size(), expectedNumOfFields);
    }

    @DataProvider
    Object[][] getSprintDetailDataProvider() {
        return new Object[][]{
                {
                        "123",
                        Sprint.builder()
                                .id(123)
                                .self("https://your-jira-instance/rest/agile/1.0/sprint/123")
                                .state("active")
                                .name("Sprint 123")
                                .startDate(LocalDateTime.parse("2023-01-01T00:00:00.000Z", formatter))
                                .endDate(LocalDateTime.parse("2023-01-15T00:00:00.000Z", formatter))
                                .activatedDate(LocalDateTime.parse("2023-01-01T00:00:00.000Z", formatter))
                                .originBoardId(456)
                                .goal("Placeholder for Sprint Goal")
                                .synced(false)
                                .build(),
                        """
                            {
                              "id": 123,
                              "self": "https://your-jira-instance/rest/agile/1.0/sprint/123",
                              "state": "active",
                              "name": "Sprint 123",
                              "startDate": "2023-01-01T00:00:00.000Z",
                              "endDate": "2023-01-15T00:00:00.000Z",
                              "activatedDate": "2023-01-01T00:00:00.000Z",
                              "originBoardId": 456,
                              "goal": "Placeholder for Sprint Goal",
                              "synced": false
                            }
                        """
                }
        };
    }

    @DataProvider
    Object[][] getSprintIssuesDataProvider() {
        return new Object[][]{
                {
                        "123",
                        List.of(
                            Issue.builder()
                                    .id(532605)
                                    .key("Issue-456")
                                    .self("https://your-jira-server.com/rest/agile/1.0/issue/532605")
                                    .build(),
                            Issue.builder()
                                    .id(1)
                                    .key("Issue-1")
                                    .self("https://your-jira-server.com/rest/agile/1.0/issue/1")
                                    .build()
                        ),
                        1,
                        """
                        {
                            "expand": "schema,names",
                            "startAt": 0,
                            "maxResults": 1,
                            "total": 2,
                            "issues": [
                                {
                                    "expand": "operations,versionedRepresentations,editmeta,changelog,renderedFields",
                                    "id": "532605",
                                    "self": "https://your-jira-server.com/rest/agile/1.0/issue/532605",
                                    "key": "Issue-456",
                                    "fields": {
                                        "customfield_10106": 3,
                                        "epic": {
                                            "id": 530674,
                                            "key": "Epic-7479",
                                            "self": "https://your-jira-server.com/rest/agile/1.0/epic/530674",
                                            "name": "Reporting UI business logic",
                                            "summary": "Implement business logic for the UI application",
                                            "color": {
                                                "key": "color_13"
                                            },
                                            "done": false
                                        },
                                        "labels": [
                                            "reporting-ui"
                                        ],
                                        "assignee": {
                                            "self": "https://your-jira-server.com/rest/api/2/user?username=jdoe",
                                            "name": "jdoe",
                                            "key": "JIRAUSER16653",
                                            "emailAddress": "jdoe@mycompany.com",
                                            "avatarUrls": {
                                                "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER16653&avatarId=13000",
                                                "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER16653&avatarId=13000",
                                                "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER16653&avatarId=13000",
                                                "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER16653&avatarId=13000"
                                            },
                                            "displayName": "John Doe",
                                            "active": true,
                                            "timeZone": "Etc/UTC"
                                        },
                                        "status": {
                                            "self": "https://your-jira-server.com/rest/api/2/status/10100",
                                            "description": "",
                                            "iconUrl": "https://your-jira-server.com/",
                                            "name": "In Review",
                                            "id": "10100",
                                            "statusCategory": {
                                                "self": "https://your-jira-server.com/rest/api/2/statuscategory/4",
                                                "id": 4,
                                                "key": "indeterminate",
                                                "colorName": "yellow",
                                                "name": "In Progress"
                                            }
                                        },
                                        "components": [
                                            {
                                                "self": "https://your-jira-server.com/rest/api/2/component/11007",
                                                "id": "11007",
                                                "name": "Backend",
                                                "description": "Backend services and APIs"
                                            }
                                        ],
                                        "creator": {
                                            "self": "https://your-jira-server.com/rest/api/2/user?username=ecruz",
                                            "name": "ecruz",
                                            "key": "JIRAUSER13512",
                                            "emailAddress": "ecruz@mycompany.com",
                                            "avatarUrls": {
                                                "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER13512&avatarId=11903",
                                                "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER13512&avatarId=11903",
                                                "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER13512&avatarId=11903",
                                                "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER13512&avatarId=11903"
                                            },
                                            "displayName": "Elena Cruz",
                                            "active": true,
                                            "timeZone": "Etc/UTC"
                                        },
                                        "subtasks": [],
                                        "reporter": {
                                            "self": "https://your-jira-server.com/rest/api/2/user?username=ecruz",
                                            "name": "ecruz",
                                            "key": "JIRAUSER13512",
                                            "emailAddress": "ecruz@mycompany.com",
                                            "avatarUrls": {
                                                "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER13512&avatarId=11903",
                                                "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER13512&avatarId=11903",
                                                "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER13512&avatarId=11903",
                                                "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER13512&avatarId=11903"
                                            },
                                            "displayName": "Elena Cruz",
                                            "active": true,
                                            "timeZone": "Etc/UTC"
                                        },
                                        "closedSprints": [
                                            {
                                                "id": 471,
                                                "self": "https://your-jira-server.com/rest/agile/1.0/sprint/471",
                                                "state": "closed",
                                                "name": "MySprint",
                                                "startDate": "2023-11-01T18:00:00.000Z",
                                                "endDate": "2023-11-15T18:00:00.000Z",
                                                "completeDate": "2023-11-15T16:36:46.434Z",
                                                "activatedDate": "2023-11-01T18:18:18.179Z",
                                                "originBoardId": 38,
                                                "goal": "New ordering system",
                                                "synced": false
                                            }
                                        ],
                                        "progress": {
                                            "progress": 0,
                                            "total": 0
                                        },
                                        "issuetype": {
                                            "self": "https://your-jira-server.com/rest/api/2/issuetype/10100",
                                            "id": "10100",
                                            "description": "A task that needs to be done.",
                                            "iconUrl": "https://your-jira-server.com/secure/viewavatar?size=xsmall&avatarId=10318&avatarType=issuetype",
                                            "name": "Task",
                                            "subtask": false,
                                            "avatarId": 10318
                                        },
                                        "sprint": {
                                            "id": 476,
                                            "self": "https://your-jira-server.com/rest/agile/1.0/sprint/476",
                                            "state": "active",
                                            "name": "MySprint #2",
                                            "startDate": "2023-11-15T18:00:00.000Z",
                                            "endDate": "2023-11-29T18:00:00.000Z",
                                            "activatedDate": "2023-11-15T18:05:04.642Z",
                                            "originBoardId": 38,
                                            "goal": "Fix all ordeting system bugs",
                                            "synced": false
                                        },
                                        "project": {
                                            "self": "https://your-jira-server.com/rest/api/2/project/10102",
                                            "id": "10102",
                                            "key": "Issue",
                                            "name": "My eShop",
                                            "projectTypeKey": "software",
                                            "avatarUrls": {
                                                "48x48": "https://your-jira-server.com/secure/projectavatar?pid=10102&avatarId=10708",
                                                "24x24": "https://your-jira-server.com/secure/projectavatar?size=small&pid=10102&avatarId=10708",
                                                "16x16": "https://your-jira-server.com/secure/projectavatar?size=xsmall&pid=10102&avatarId=10708",
                                                "32x32": "https://your-jira-server.com/secure/projectavatar?size=medium&pid=10102&avatarId=10708"
                                            }
                                        },
                                        "created": "2023-10-27T08:05:50.000+0000",
                                        "updated": "2023-11-17T22:10:59.000+0000",
                                        "description": "As a new user, I want to be able to register for the online platform easily so that I can access personalized features and content.",
                                        "attachment": [],
                                        "summary": "Add ability to call API with UI"
                                    }
                                }
                            ]
                        }
                        """,
                        """
                        {
                            "expand": "schema,names",
                            "startAt": 1,
                            "maxResults": 1,
                            "total": 2,
                            "issues": [
                                {
                                    "expand": "operations,versionedRepresentations,editmeta,changelog,renderedFields",
                                    "id": "1",
                                    "self": "https://your-jira-server.com/rest/agile/1.0/issue/1",
                                    "key": "Issue-1",
                                    "fields": {
                                        "customfield_10106": 3,
                                        "epic": {
                                            "id": 530674,
                                            "key": "Epic-7479",
                                            "self": "https://your-jira-server.com/rest/agile/1.0/epic/530674",
                                            "name": "Reporting UI business logic",
                                            "summary": "Implement business logic for the UI application",
                                            "color": {
                                                "key": "color_13"
                                            },
                                            "done": false
                                        },
                                        "labels": [
                                            "reporting-ui"
                                        ],
                                        "assignee": {
                                            "self": "https://your-jira-server.com/rest/api/2/user?username=jdoe",
                                            "name": "jdoe",
                                            "key": "JIRAUSER16653",
                                            "emailAddress": "jdoe@mycompany.com",
                                            "avatarUrls": {
                                                "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER16653&avatarId=13000",
                                                "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER16653&avatarId=13000",
                                                "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER16653&avatarId=13000",
                                                "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER16653&avatarId=13000"
                                            },
                                            "displayName": "John Doe",
                                            "active": true,
                                            "timeZone": "Etc/UTC"
                                        },
                                        "status": {
                                            "self": "https://your-jira-server.com/rest/api/2/status/10100",
                                            "description": "",
                                            "iconUrl": "https://your-jira-server.com/",
                                            "name": "In Review",
                                            "id": "10100",
                                            "statusCategory": {
                                                "self": "https://your-jira-server.com/rest/api/2/statuscategory/4",
                                                "id": 4,
                                                "key": "indeterminate",
                                                "colorName": "yellow",
                                                "name": "In Progress"
                                            }
                                        },
                                        "components": [
                                            {
                                                "self": "https://your-jira-server.com/rest/api/2/component/11007",
                                                "id": "11007",
                                                "name": "Backend",
                                                "description": "Backend services and APIs"
                                            }
                                        ],
                                        "creator": {
                                            "self": "https://your-jira-server.com/rest/api/2/user?username=ecruz",
                                            "name": "ecruz",
                                            "key": "JIRAUSER13512",
                                            "emailAddress": "ecruz@mycompany.com",
                                            "avatarUrls": {
                                                "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER13512&avatarId=11903",
                                                "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER13512&avatarId=11903",
                                                "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER13512&avatarId=11903",
                                                "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER13512&avatarId=11903"
                                            },
                                            "displayName": "Elena Cruz",
                                            "active": true,
                                            "timeZone": "Etc/UTC"
                                        },
                                        "subtasks": [],
                                        "reporter": {
                                            "self": "https://your-jira-server.com/rest/api/2/user?username=ecruz",
                                            "name": "ecruz",
                                            "key": "JIRAUSER13512",
                                            "emailAddress": "ecruz@mycompany.com",
                                            "avatarUrls": {
                                                "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER13512&avatarId=11903",
                                                "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER13512&avatarId=11903",
                                                "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER13512&avatarId=11903",
                                                "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER13512&avatarId=11903"
                                            },
                                            "displayName": "Elena Cruz",
                                            "active": true,
                                            "timeZone": "Etc/UTC"
                                        },
                                        "closedSprints": [
                                            {
                                                "id": 471,
                                                "self": "https://your-jira-server.com/rest/agile/1.0/sprint/471",
                                                "state": "closed",
                                                "name": "MySprint",
                                                "startDate": "2023-11-01T18:00:00.000Z",
                                                "endDate": "2023-11-15T18:00:00.000Z",
                                                "completeDate": "2023-11-15T16:36:46.434Z",
                                                "activatedDate": "2023-11-01T18:18:18.179Z",
                                                "originBoardId": 38,
                                                "goal": "New ordering system",
                                                "synced": false
                                            }
                                        ],
                                        "progress": {
                                            "progress": 0,
                                            "total": 0
                                        },
                                        "issuetype": {
                                            "self": "https://your-jira-server.com/rest/api/2/issuetype/10100",
                                            "id": "10100",
                                            "description": "A task that needs to be done.",
                                            "iconUrl": "https://your-jira-server.com/secure/viewavatar?size=xsmall&avatarId=10318&avatarType=issuetype",
                                            "name": "Task",
                                            "subtask": false,
                                            "avatarId": 10318
                                        },
                                        "sprint": {
                                            "id": 476,
                                            "self": "https://your-jira-server.com/rest/agile/1.0/sprint/476",
                                            "state": "active",
                                            "name": "MySprint #2",
                                            "startDate": "2023-11-15T18:00:00.000Z",
                                            "endDate": "2023-11-29T18:00:00.000Z",
                                            "activatedDate": "2023-11-15T18:05:04.642Z",
                                            "originBoardId": 38,
                                            "goal": "Fix all ordeting system bugs",
                                            "synced": false
                                        },
                                        "project": {
                                            "self": "https://your-jira-server.com/rest/api/2/project/10102",
                                            "id": "10102",
                                            "key": "Issue",
                                            "name": "My eShop",
                                            "projectTypeKey": "software",
                                            "avatarUrls": {
                                                "48x48": "https://your-jira-server.com/secure/projectavatar?pid=10102&avatarId=10708",
                                                "24x24": "https://your-jira-server.com/secure/projectavatar?size=small&pid=10102&avatarId=10708",
                                                "16x16": "https://your-jira-server.com/secure/projectavatar?size=xsmall&pid=10102&avatarId=10708",
                                                "32x32": "https://your-jira-server.com/secure/projectavatar?size=medium&pid=10102&avatarId=10708"
                                            }
                                        },
                                        "created": "2023-10-27T08:05:50.000+0000",
                                        "updated": "2023-11-17T22:10:59.000+0000",
                                        "description": "As a new user, I want to be able to register for the online platform easily so that I can access personalized features and content.",
                                        "attachment": [],
                                        "summary": "Add ability to call API with UI"
                                    }
                                }
                            ]
                        }
                        """
                }
        };
    }



    @DataProvider
    Object[][] getIssueDetailDataProvider() {
        return new Object[][]{
                {
                        "Issue-456",
                        Issue.builder()
                                .id(532605)
                                .key("Issue-456")
                                .self("https://your-jira-server.com/rest/agile/1.0/issue/532605")
                                .build(),
                        19,
                        """
                        {
                            "expand": "operations,versionedRepresentations,editmeta,changelog,renderedFields",
                            "id": "532605",
                            "self": "https://your-jira-server.com/rest/agile/1.0/issue/532605",
                            "key": "Issue-456",
                            "fields": {
                                "customfield_10106": 3,                                
                                "epic": {
                                    "id": 530674,
                                    "key": "Epic-7479",
                                    "self": "https://your-jira-server.com/rest/agile/1.0/epic/530674",
                                    "name": "Reporting UI business logic",
                                    "summary": "Implement business logic for the UI application",
                                    "color": {
                                        "key": "color_13"
                                    },
                                    "done": false
                                },                                                                
                                "labels": [
                                    "reporting-ui"
                                ],                                                                
                                "assignee": {
                                    "self": "https://your-jira-server.com/rest/api/2/user?username=jdoe",
                                    "name": "jdoe",
                                    "key": "JIRAUSER16653",
                                    "emailAddress": "jdoe@mycompany.com",
                                    "avatarUrls": {
                                        "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER16653&avatarId=13000",
                                        "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER16653&avatarId=13000",
                                        "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER16653&avatarId=13000",
                                        "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER16653&avatarId=13000"
                                    },
                                    "displayName": "John Doe",
                                    "active": true,
                                    "timeZone": "Etc/UTC"
                                },
                                "status": {
                                    "self": "https://your-jira-server.com/rest/api/2/status/10100",
                                    "description": "",
                                    "iconUrl": "https://your-jira-server.com/",
                                    "name": "In Review",
                                    "id": "10100",
                                    "statusCategory": {
                                        "self": "https://your-jira-server.com/rest/api/2/statuscategory/4",
                                        "id": 4,
                                        "key": "indeterminate",
                                        "colorName": "yellow",
                                        "name": "In Progress"
                                    }
                                },
                                "components": [
                                    {
                                        "self": "https://your-jira-server.com/rest/api/2/component/11007",
                                        "id": "11007",
                                        "name": "Backend",
                                        "description": "Backend services and APIs"
                                    }
                                ],                                
                                "creator": {
                                    "self": "https://your-jira-server.com/rest/api/2/user?username=ecruz",
                                    "name": "ecruz",
                                    "key": "JIRAUSER13512",
                                    "emailAddress": "ecruz@mycompany.com",
                                    "avatarUrls": {
                                        "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER13512&avatarId=11903",
                                        "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER13512&avatarId=11903",
                                        "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER13512&avatarId=11903",
                                        "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER13512&avatarId=11903"
                                    },
                                    "displayName": "Elena Cruz",
                                    "active": true,
                                    "timeZone": "Etc/UTC"
                                },
                                "subtasks": [],
                                "reporter": {
                                    "self": "https://your-jira-server.com/rest/api/2/user?username=ecruz",
                                    "name": "ecruz",
                                    "key": "JIRAUSER13512",
                                    "emailAddress": "ecruz@mycompany.com",
                                    "avatarUrls": {
                                        "48x48": "https://your-jira-server.com/secure/useravatar?ownerId=JIRAUSER13512&avatarId=11903",
                                        "24x24": "https://your-jira-server.com/secure/useravatar?size=small&ownerId=JIRAUSER13512&avatarId=11903",
                                        "16x16": "https://your-jira-server.com/secure/useravatar?size=xsmall&ownerId=JIRAUSER13512&avatarId=11903",
                                        "32x32": "https://your-jira-server.com/secure/useravatar?size=medium&ownerId=JIRAUSER13512&avatarId=11903"
                                    },
                                    "displayName": "Elena Cruz",
                                    "active": true,
                                    "timeZone": "Etc/UTC"
                                },                                
                                "closedSprints": [
                                    {
                                        "id": 471,
                                        "self": "https://your-jira-server.com/rest/agile/1.0/sprint/471",
                                        "state": "closed",
                                        "name": "MySprint",
                                        "startDate": "2023-11-01T18:00:00.000Z",
                                        "endDate": "2023-11-15T18:00:00.000Z",
                                        "completeDate": "2023-11-15T16:36:46.434Z",
                                        "activatedDate": "2023-11-01T18:18:18.179Z",
                                        "originBoardId": 38,
                                        "goal": "New ordering system",
                                        "synced": false
                                    }
                                ],
                                "progress": {
                                    "progress": 0,
                                    "total": 0
                                },
                                "issuetype": {
                                    "self": "https://your-jira-server.com/rest/api/2/issuetype/10100",
                                    "id": "10100",
                                    "description": "A task that needs to be done.",
                                    "iconUrl": "https://your-jira-server.com/secure/viewavatar?size=xsmall&avatarId=10318&avatarType=issuetype",
                                    "name": "Task",
                                    "subtask": false,
                                    "avatarId": 10318
                                },                                
                                "sprint": {
                                    "id": 476,
                                    "self": "https://your-jira-server.com/rest/agile/1.0/sprint/476",
                                    "state": "active",
                                    "name": "MySprint #2",
                                    "startDate": "2023-11-15T18:00:00.000Z",
                                    "endDate": "2023-11-29T18:00:00.000Z",
                                    "activatedDate": "2023-11-15T18:05:04.642Z",
                                    "originBoardId": 38,
                                    "goal": "Fix all ordeting system bugs",
                                    "synced": false
                                },
                                "project": {
                                    "self": "https://your-jira-server.com/rest/api/2/project/10102",
                                    "id": "10102",
                                    "key": "Issue",
                                    "name": "My eShop",
                                    "projectTypeKey": "software",
                                    "avatarUrls": {
                                        "48x48": "https://your-jira-server.com/secure/projectavatar?pid=10102&avatarId=10708",
                                        "24x24": "https://your-jira-server.com/secure/projectavatar?size=small&pid=10102&avatarId=10708",
                                        "16x16": "https://your-jira-server.com/secure/projectavatar?size=xsmall&pid=10102&avatarId=10708",
                                        "32x32": "https://your-jira-server.com/secure/projectavatar?size=medium&pid=10102&avatarId=10708"
                                    }
                                },
                                "created": "2023-10-27T08:05:50.000+0000",                                
                                "updated": "2023-11-17T22:10:59.000+0000",
                                "description": "As a new user, I want to be able to register for the online platform easily so that I can access personalized features and content.",
                                "attachment": [],
                                "summary": "Add ability to call API with UI"
                            }
                        }                     
                        """
                }
        };
    }

    private void mockResponse(String url, String expectedResponse) throws Exception {
        CloseableHttpResponse responseMock = Mockito.mock(CloseableHttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        when(responseMock.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK"));
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(expectedResponse.getBytes()));
        when(responseMock.getEntity()).thenReturn(entity);
        when(httpClientMock.execute(Mockito.argThat(request ->
                request instanceof HttpGet && request.getURI().toString().equals(url))))
                .thenReturn(responseMock);
    }
}