package com.scanin.controller;

import com.scanin.config.JwtService;
import com.scanin.dao.entity.User;
import com.scanin.dto.request.ActivateAccountRequest;
import com.scanin.dto.request.LoginRequest;
import com.scanin.dto.response.AuthResponse;
import com.scanin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Autentifikasiya (Auth)", description = "İşçilərin ilk dəfə hesabını aktivləşdirməsi və sistemə giriş (JWT Token almaq) əməliyyatları")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    @Operation(
            summary = "İşçi hesabının ilk aktivləşdirilməsi (Öz username və şifrəsini təyin edir)",
            description = "HR işçini sistemə əlavə etdikdən sonra işçiyə verilən `EMP-XXX` kodu vasitəsilə işçi öz istədiyi `username` (məs: nurka) və şifrəni təyin edir. Cavab olaraq dərhal 1 illik JWT Token qaytarılır."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hesab uğurla aktivləşdirildi və JWT Token verildi",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validasiya xətası, hesab artıq aktivləşdirilib və ya seçilmiş username başqası tərəfindən tutulub",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Daxil edilən İşçi Kodu (EMP-XXX) bazada tapılmadı",
                    content = @Content)
    })
    @PostMapping("/activate")
    public ResponseEntity<AuthResponse> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        User activatedUser = userService.activateEmployeeAccount(request);
        String token = jwtService.generateToken(activatedUser.getUsername(), activatedUser.getRole().name());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .username(activatedUser.getUsername())
                .fullName(activatedUser.getFullName())
                .position(activatedUser.getPosition())
                .role(activatedUser.getRole())
                .build());
    }

    @Operation(
            summary = "Sistemə Giriş (Login)",
            description = "Artıq aktivləşdirilmiş işçi hesabı və ya HR/Admin hesabı ilə sistemə daxil olub JWT Token almaq üçün istifadə olunur. (İlk Admin: username=`admin`, password=`admin123`)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uğurlu giriş, JWT Token qaytarıldı",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Boş sahələr göndərilib (Validasiya xətası)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "İstifadəçi adı və ya şifrə yanlışdır",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String cleanUsername = request.getUsername().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(cleanUsername, request.getPassword())
        );

        User user = userService.getByUsername(cleanUsername);
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .position(user.getPosition())
                .role(user.getRole())
                .build());
    }
}