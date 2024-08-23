package org.korecky.jiracli.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sprint implements Serializable {
    int id;
    String self;
    String state;
    String name;
    LocalDateTime startDate;
    LocalDateTime endDate;
    LocalDateTime activatedDate;
    int originBoardId;
    String goal;
    boolean synced;
}
