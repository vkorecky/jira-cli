package org.korecky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintIssues {
    String expand;
    int startAt;
    int maxResults;
    int total;
    List<Issue> issues;
}
