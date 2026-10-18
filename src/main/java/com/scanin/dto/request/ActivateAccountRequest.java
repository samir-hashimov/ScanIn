package com.scanin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivateAccountRequest {

    @NotBlank(message = "HR tərəfindən verilən İşçi Kodu (məs: EMP-001) boş ola bilməz!")
    private String employeeCode;

    @NotBlank(message = "İstifadəçi adı (username) boş ola bilməz!")
    @Size(min = 3, max = 50, message = "İstifadəçi adı 3 ilə 50 simvol arasında olmalıdır!")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "İstifadəçi adında yalnız hərf, rəqəm, nöqtə və xətt ola bilər (məs: samir.h)!")
    private String username;

    @NotBlank(message = "Şifrə boş ola bilməz!")
    @Size(min = 6, max = 100, message = "Şifrə ən azı 6 simvoldan ibarət olmalıdır!")
    private String password;
}