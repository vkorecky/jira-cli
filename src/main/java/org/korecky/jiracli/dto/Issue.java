package org.korecky.jiracli.dto;

import com.fasterxml.jackson.databind.JsonNode;
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
public class Issue implements Serializable {
    int id;
    String key;
    String self;
    Assignee assignee;
    List<String> labels;
    List<Component> components;
    JsonNode fields;
}
