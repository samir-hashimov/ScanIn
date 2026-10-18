package com.scanin.dto.response;

import com.scanin.util.Role;
import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String employeeCode;
    private String username;
    private String fullName;
    private String position;
    private Role role;
    private LocalTime shiftStartTime;
    private LocalTime shiftEndTime;
}