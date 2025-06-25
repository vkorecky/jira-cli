package org.korecky.jiracli.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Board {
    private int id;
    private String name;
    private String type;
}