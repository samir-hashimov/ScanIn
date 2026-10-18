package com.scanin.dto.response;

import com.scanin.util.ArrivalStatus;
import com.scanin.util.DepartureStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ScanResponse {
    // Ekranda çıxacaq əyləncəli mesaj ("🥇 Günün çempionu!..." və ya "👀 Dayan görüm, hara gedirsən?")
    private String message;

    // İşçinin adı və vəzifəsi ("Samir Həşimov - Backend Developer")
    private String employeeInfo;

    // Əməliyyatın növü: "CHECK_IN", "CHECK_OUT", "COOLDOWN_WARNING" (10 dəq xəbərdarlığı)
    private String actionType;

    // 30+ dəq gecikibsə true olacaq -> Frontend bunu görüb ekranda "Səbəb yaz" xanasını açacaq!
    private boolean requiresLateReason;

    // 10 dəq içində təkrar oxudubsa true olacaq -> Frontend "Səhvən basdım / Təcili çıxıram" seçimini açacaq!
    private boolean requiresEarlyLeaveConfirmation;

    private LocalDateTime scanTime;
    private ArrivalStatus arrivalStatus;
    private DepartureStatus departureStatus;
}