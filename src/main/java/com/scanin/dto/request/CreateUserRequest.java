package com.scanin.dto.request;

import com.scanin.util.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CreateUserRequest {

    @NotBlank(message = "İşçinin tam adı (Ad və Soyad) boş ola bilməz!")
    @Size(min = 3, max = 100, message = "Ad və Soyad 3 ilə 100 simvol arasında olmalıdır!")
    private String fullName;

    @NotBlank(message = "İşçinin vəzifəsi (position) mütləq qeyd olunmalıdır!")
    @Size(min = 2, max = 100, message = "Vəzifə adı 2 ilə 100 simvol arasında olmalıdır!")
    private String position;

    private Role role;
    private LocalTime shiftStartTime;
    private LocalTime shiftEndTime;
}