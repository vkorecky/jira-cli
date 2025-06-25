package org.korecky.jiracli.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardList {
    @JsonProperty("maxResults")
    private int maxResults;

    @JsonProperty("startAt")
    private int startAt;

    @JsonProperty("isLast")
    private boolean isLast;

    @JsonProperty("values")
    private List<Board> values;
}