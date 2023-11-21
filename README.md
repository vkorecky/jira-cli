# jira-cli
Java application for downloading information from JIRA

## Configuration
Create configuration file "{YOUR_HOME_FOLDER}/.jira-cli/config.yml". And modify the content:

``` yaml
sprintId: 1
outputFolder: "/YOUR_OUTPUT_FOLDER"
storyPointsColumn: "customfield_123"

server:
url: "https://your-jira-server.com/"
username: "username"
password: "password"

teams:
- name: "backend"
  members:
    - user1@mycompany.com
    - user2@mycompany.com
- name: "frontend"
  members:
    - user3@mycompany.com
- name: "qa"
  members:
    - user4@mycompany.com
    - user5@mycompany.com
- name: "my_next_team"
  members:
    - user6@mycompany.com
    - user7@mycompany.com
```

