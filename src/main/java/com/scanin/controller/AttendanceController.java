package com.scanin.controller;

import com.scanin.dto.request.ScanActionRequest;
import com.scanin.dto.response.ScanResponse;
import com.scanin.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "2. QR Kod Oxutma (Attendance)", description = "İşçilərin gündəlik işə giriş (Check-In) və işdən çıxış (Check-Out) əməliyyatları")
@SecurityRequirement(name = "bearerAuth")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(
            summary = "QR Kod Oxutma (Giriş / Çıxış / Gecikmə Səbəbi)",
            description = """
                    İşçi qapıdakı QR kodu oxutduqda bu endpoint çağırılır. İşçinin kimliyi avtomatik olaraq **JWT Token**-dən götürülür.
                    - **1-ci oxutma (Səhər):** Giriş (`CHECK_IN`) qeydə alınır. İlk gələndirsə çempion mesajı verilir.
                    - **30+ dəq gecikmə:** `requiresLateReason = true` qayıdır və işçi `reason` göndərərək gecikmə səbəbini qeyd edir.
                    - **İlk 10 dəqiqə içində təkrar oxutma:** `COOLDOWN_WARNING` qayıdır. İşçi həqiqətən çıxırsa `forceEarlyCheckout = true` və `reason` göndərir.
                    - **Axşam oxutma:** Çıxış (`CHECK_OUT`) və işlədiyi saatlar qeydə alınır. Son çıxandırsa xüsusi təşəkkür mesajı verilir.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "QR oxutma uğurla emal edildi (Cavabdakı actionType və mesajlara baxın)",
                    content = @Content(schema = @Schema(implementation = ScanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Gecikmə və ya erkən çıxış səbəbi 5 simvoldan azdır (Validasiya xətası)",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "JWT Token göndərilməyib və ya vaxtı bitib",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "İşçi Ofis Wi-Fi şəbəkəsində deyil (IP uyğunsuzluğu)!",
                    content = @Content)
    })
    @PostMapping("/scan")
    public ResponseEntity<ScanResponse> scanQrCode(
            Authentication authentication,
            @Valid @RequestBody(required = false) ScanActionRequest actionRequest,
            HttpServletRequest httpRequest) {

        String loggedInUsername = authentication.getName();
        ScanResponse response = attendanceService.processQrScan(loggedInUsername, actionRequest, httpRequest);
        return ResponseEntity.ok(response);
    }
}