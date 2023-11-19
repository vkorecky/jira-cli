package org.korecky;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Objects;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintDetail implements Serializable {
    public static final String FORMAT_STRING_NO_TZ = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String TIME_ZONE = "UTC";

    @JsonProperty("id")
    private int id;

    @JsonProperty("self")
    private String self;

    @JsonProperty("state")
    private String state;

    @JsonProperty("name")
    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = FORMAT_STRING_NO_TZ, timezone = TIME_ZONE)
    @JsonProperty("startDate")
    private DateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = FORMAT_STRING_NO_TZ, timezone = TIME_ZONE)
    @JsonProperty("endDate")
    private DateTime endDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = FORMAT_STRING_NO_TZ, timezone = TIME_ZONE)
    @JsonProperty("activatedDate")
    private DateTime activatedDate;

    @JsonProperty("originBoardId")
    private int originBoardId;

    @JsonProperty("goal")
    private String goal;

    @JsonProperty("synced")
    private boolean synced;

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SprintDetail sprintDetail = (SprintDetail) o;
        if (id != sprintDetail.id) return false;
        if (!Objects.equals(self, sprintDetail.self)) return false;
        if (!Objects.equals(state, sprintDetail.state)) return false;
        if (!Objects.equals(name, sprintDetail.name)) return false;
        if (!Objects.equals(startDate, sprintDetail.startDate)) return false;
        if (!Objects.equals(endDate, sprintDetail.endDate)) return false;
        if (!Objects.equals(activatedDate, sprintDetail.activatedDate)) return false;
        if (originBoardId != sprintDetail.originBoardId) return false;
        if (!Objects.equals(goal, sprintDetail.goal)) return false;
        return synced == sprintDetail.synced;
    }
}
