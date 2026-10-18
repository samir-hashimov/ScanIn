package com.scanin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "İstifadəçi adı boş ola bilməz!")
    private String username;

    @NotBlank(message = "Şifrə boş ola bilməz!")
    private String password;
}