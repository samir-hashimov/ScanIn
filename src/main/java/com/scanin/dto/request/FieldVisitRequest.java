package com.scanin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FieldVisitRequest {
    @NotBlank(message = "Kontragentin adını yazmaq məcburidir!")
    private String counterpartyName;

    private String note;
}