package org.korecky.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {
    private int sprintId;
    private String outputFolder;
    private String storyPointsColumn;
    private Server server;
    private List<Team> teams;
}
