plugins {
    id("java")
    kotlin("kapt") version "1.9.20"
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
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
    testImplementation("junit:junit:4.13.1")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.testng:testng:7.8.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    implementation("com.atlassian.jira:jira-rest-java-client-core:5.2.4")
    implementation("com.atlassian.jira:jira-rest-java-client-api:5.2.4")
    implementation("commons-cli:commons-cli:1.6.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.16.0")
}

tasks.test {
    useTestNG()
}