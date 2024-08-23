package org.korecky.jiracli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assignee implements Serializable {
    int id;
    String key;
    String name;
    String emailAddress;
    String displayName;
    Boolean active;
}
