package com.scanin.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FieldVisitRequest {

    @Size(max = 150, message = "Kontragent adı 150 simvoldan uzun ola bilməz")
    private String counterpartyName;

    @Size(max = 500, message = "Qeyd 500 simvoldan uzun ola bilməz")
    private String note;

    @Size(max = 500, message = "Ünvan qeydi 500 simvoldan uzun ola bilməz")
    private String locationNote;

    @DecimalMin(value = "-90.0", message = "Latitude -90 ilə 90 arasında olmalıdır")
    @DecimalMax(value = "90.0", message = "Latitude -90 ilə 90 arasında olmalıdır")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude -180 ilə 180 arasında olmalıdır")
    @DecimalMax(value = "180.0", message = "Longitude -180 ilə 180 arasında olmalıdır")
    private Double longitude;
}
