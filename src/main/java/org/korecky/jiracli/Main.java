package org.korecky.jiracli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.korecky.jiracli.configuration.Configuration;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Configuration configuration = loadConfig();
        Reports reports = new Reports(configuration);
        reports.generate();
    }

    private static Configuration loadConfig() throws IOException {
        String homeFolder = System.getProperty("user.home");
        String configFilePath = homeFolder + File.separator + ".jira-cli" + File.separator + "config.yml";

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(new File(configFilePath), Configuration.class);
    }
}