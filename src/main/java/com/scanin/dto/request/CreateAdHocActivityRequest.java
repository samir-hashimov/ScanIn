package com.scanin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAdHocActivityRequest {

    @NotBlank(message = "Fəaliyyətin adı boş ola bilməz!")
    private String title;

    private String description;

    /** Ayrılan vaxt (dəqiqə). Boşdursa 60 dəqiqə götürülür. */
    private Integer allocatedMinutes;

    /** Fasilə verilən əsas tapşırığın ID-si (opsional) */
    private Long pausedTaskId;
}
