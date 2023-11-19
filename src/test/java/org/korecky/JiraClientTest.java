package org.korecky;

import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.joda.time.DateTime;
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

import static org.mockito.Mockito.when;

public class JiraClientTest {

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

    @Test(dataProvider = "getSprintDetailsDataProvider")
    public void getSprintDetails(String sprintId, SprintDetail expectedSprintDetail, String responseJson) throws Exception {
        String url = "https://your-jira-server.com/rest/agile/1.0/sprint/" + sprintId;
        mockResponse(url, responseJson);

        SprintDetail actualResponse = jiraClient.getSprintDetails(sprintId);
        Assert.assertEquals(actualResponse, expectedSprintDetail);
    }

    @Test(dataProvider = "getIssueDetailsDataProvider")
    public void getIssueDetails(String issueKey, String expectedResponse) throws Exception {
        String url = "https://your-jira-server.com/rest/api/2/issue/" + issueKey;
        mockResponse(url, expectedResponse);

        String actualResponse = jiraClient.getIssueDetails(issueKey);
        Assert.assertEquals(actualResponse, expectedResponse);
    }

    @DataProvider
    Object[][] getSprintDetailsDataProvider() {
        return new Object[][]{
                {
                        "123",
                        SprintDetail.builder()
                                .id(123)
                                .self("https://your-jira-instance/rest/agile/1.0/sprint/123")
                                .state("active")
                                .name("Sprint 123")
                                .startDate(new DateTime("2023-01-01T00:00:00.000Z"))
                                .endDate(new DateTime("2023-01-15T00:00:00.000Z"))
                                .activatedDate(new DateTime("2023-01-01T00:00:00.000Z"))
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
    Object[][] getIssueDetailsDataProvider() {
        return new Object[][]{
                {
                        "Issue-123",
                        "Issue-123"
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