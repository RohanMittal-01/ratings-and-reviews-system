package com.ratingsandreviews.rating;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class ApplicationRatingStatsId implements Serializable {
    private UUID applicationId;
    private short scale;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationRatingStatsId that = (ApplicationRatingStatsId) o;
        return scale == that.scale && Objects.equals(applicationId, that.applicationId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(applicationId, scale);
    }
}

