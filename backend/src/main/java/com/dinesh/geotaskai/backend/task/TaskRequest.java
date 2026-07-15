package com.dinesh.geotaskai.backend.task;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TaskRequest(
    @NotBlank(message = "Title is required")
    String title,

    String notes,

    @NotBlank(message = "Location name is required")
    String locationName,

    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "(?i)low|medium|high", message = "Priority must be Low, Medium, or High")
    String priority,

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
    @DecimalMax(value = "90.0", message = "Latitude must be at most 90")
    Double latitude,

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
    @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
    Double longitude,

    @NotNull(message = "Radius is required")
    @Positive(message = "Radius must be greater than 0")
    Double radiusMeters
) {
}
