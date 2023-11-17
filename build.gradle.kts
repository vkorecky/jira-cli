plugins {
    id("java")
}

group = "org.korecky"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.atlassian.com/content/repositories/atlassian-public/")
    }
    maven {
        url = uri("https://packages.atlassian.com/mvn/maven-atlassian-external/")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.atlassian.jira:jira-rest-java-client-core:5.2.4")
    implementation("com.atlassian.jira:jira-rest-java-client-api:5.2.4")
}

tasks.test {
    useJUnitPlatform()
}