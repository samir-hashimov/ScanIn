package com.scanin.controller;

import com.scanin.dto.request.CreateUserRequest;
import com.scanin.dto.response.AttendanceReportResponse;
import com.scanin.dto.response.UserResponse;
import com.scanin.scheduler.AttendanceScheduler;
import com.scanin.service.AttendanceService;
import com.scanin.service.OfficeIpService;
import com.scanin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@Tag(name = "3. HR və Admin Paneli", description = "Yeni işçilərin yaradılması, günlük giriş-çıxış hesabatları, ofis IP yenilənməsi və Scheduler idarəetməsi (Yalnız HR və ADMIN rolları üçün)")
@SecurityRequirement(name = "bearerAuth")
public class HrAdminController {

    private final UserService userService;
    private final AttendanceService attendanceService;
    private final OfficeIpService officeIpService;
    private final AttendanceScheduler attendanceScheduler;

    @Operation(
            summary = "Yeni işçi əlavə etmək (Avtomatik EMP-XXX kodu yaradır)",
            description = "HR yalnız işçinin Ad-Soyadını, Vəzifəsini və İş saatlarını daxil edir. Sistem avtomatik olaraq unikal `EMP-001` tipli kod yaradır. İşçi həmin kodla öz hesabını aktivləşdirir."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "İşçi uğurla yaradıldı və EMP kodu təyin edildi",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Məlumatlar yanlışdır (məs: ad boşdur və ya çıxış saatı giriş saatından əvvəldir)",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Bu əməliyyat üçün HR və ya ADMIN səlahiyyəti tələb olunur",
                    content = @Content)
    })
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);

    }

    @Operation(
            summary = "Bütün işçilərin siyahısını əldə etmək",
            description = "Sistemdə qeydiyyatda olan bütün işçiləri, onların EMP kodlarını, vəzifələrini və hesablarını aktivləşdirib-aktivləşdirmədiklərini (username olub-olmadığını) qaytarır."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "İşçilərin siyahısı uğurla gətirildi",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Səlahiyyət xətası (Yalnız HR və ya ADMIN)",
                    content = @Content)
    })
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(
            summary = "Günlük Davamiyyət (Giriş-Çıxış) Hesabatı",
            description = "Seçilmiş tarix üzrə bütün işçilərin giriş-çıxış saatlarını, gecikmə dəqiqələrini, əlavə iş (overtime) vaxtlarını, ofisə ilk gələni və ən son çıxanı qaytarır. Tarix göndərilməzsə, avtomatik **bu günün** hesabatını qaytarır."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Günlük hesabat siyahısı uğurla gətirildi",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AttendanceReportResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Səlahiyyət xətası (Yalnız HR və ya ADMIN)",
                    content = @Content)
    })
    @GetMapping("/reports/daily")
    public ResponseEntity<List<AttendanceReportResponse>> getDailyReport(
            @Parameter(description = "Hesabat tarixi (Format: YYYY-MM-DD, məsələn: 2026-10-17). Boş buraxılsa bu gün götürülür.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(attendanceService.getDailyReport(date));
    }

    @Operation(
            summary = "Ofisin Dinamik Wi-Fi IP ünvanını yeniləmək",
            description = "Əgər ofisdə Statik IP yoxdursa (`office.ip.is-static=false`), modem yeniləndikdə HR və ya ofisdəki avtomatik script gizli açar (`secret`) vasitəsilə bu endpoint-ə sorğu ataraq ofisin cari IP ünvanını bazada yeniləyir."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ofis IP ünvanı uğurla yeniləndi"),
            @ApiResponse(responseCode = "400", description = "Göndərilən gizli açar (secret key) yanlışdır")
    })
    @PostMapping("/office-ip/update")
    public ResponseEntity<Map<String, String>> updateOfficeIp(
            @Parameter(description = "application.properties-də təyin edilmiş gizli şifrə (məs: ScanInSecret2026)", required = true)
            @RequestParam String secret,
            @Parameter(description = "Yeniləməni kim etdi? (Məs: HR_ADMIN və ya AUTO_SCRIPT)")
            @RequestParam(defaultValue = "HR_ADMIN") String updatedBy,
            HttpServletRequest request) {

        String newIp = officeIpService.updateDynamicOfficeIp(request, secret, updatedBy);
        return ResponseEntity.ok(Map.of(
                "message", "✅ Ofisin yeni Wi-Fi IP ünvanı uğurla yadda saxlanıldı!",
                "currentOfficeIp", newIp
        ));
    }

    @Operation(
            summary = "Çıxışı unudan işçiləri bağlayan Scheduler-i əllə işə salmaq (Test üçün)",
            description = "Normalda hər gecə saat 23:59-da avtomatik işləyən metodu gecəni gözləmədən dərhal test etmək üçün istifadə olunur. Açıq qalmış (`WORKING`) qeydləri `FORGOT_CHECKOUT` statusuna keçirir."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Açıq qalmış iş günləri avtomatik bağlandı"),
            @ApiResponse(responseCode = "403", description = "Səlahiyyət xətası (Yalnız HR və ya ADMIN)")
    })
    @PostMapping("/scheduler/trigger-auto-close")
    public ResponseEntity<Map<String, String>> triggerAutoCloseManually() {
        attendanceScheduler.autoCloseForgottenCheckouts();
        return ResponseEntity.ok(Map.of(
                "message", "🕚 Çıxışı unudan işçilərin günü avtomatik bağlandı (FORGOT_CHECKOUT)!"
        ));
    }
}