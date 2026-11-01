package com.scanin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "İşçi kodu (məs: EMP-001) mütləqdir!")
    private String employeeCode;

    @NotBlank(message = "Tapşırığın adı boş ola bilməz!")
    private String title;

    private String description;

    @NotNull(message = "Son tarix (Deadline) mütləq göstərilməlidir!")
    private LocalDateTime deadline;

    private String external1cId; // 1C qoşulanda istifadə olunacaq
}