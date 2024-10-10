package com.alex.d.springbootatm.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2024-03-04T12:00:00Z")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(description = "HTTP status code of the error", example = "4XX")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Description of the error", example = "Error description")
    @JsonProperty("error")
    private String error;

    @Schema(description = "Path where the error occurred", example = "/endpoint")
    @JsonProperty("path")
    private String path;
}