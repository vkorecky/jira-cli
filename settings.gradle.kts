rootProject.name = "jira-cli"
include("src:test:hava")
findProject(":src:test:hava")?.name = "hava"
