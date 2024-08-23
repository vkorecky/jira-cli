package org.korecky.jiracli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintIssues implements Serializable {
    String expand;
    int startAt;
    int maxResults;
    int total;
    List<Issue> issues;
}
