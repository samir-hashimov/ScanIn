package com.scanin.service;

import com.scanin.dao.entity.OfficeConfig;
import com.scanin.dao.repository.OfficeConfigRepository;
import com.scanin.exception.InvalidIpException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OfficeIpService {

    private final OfficeConfigRepository officeConfigRepository;

    @Value("${office.ip.is-static:true}")
    private boolean isStaticIp;

    @Value("${office.ip.static-value:127.0.0.1}")
    private String staticIpValue;

    @Value("${office.ip.secret-key:ScanInSecret2026}")
    private String secretKey;

    // 1. İşçi QR oxudanda IP-sini yoxlayan əsas metod
    public void validateClientIp(HttpServletRequest request) {
        String clientIp = extractClientIp(request);

        // Lokal kompüterdə test edərkən IPv6 localhost gələ bilər, onu 127.0.0.1-ə bərabər tuturuq:
        if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
            clientIp = "127.0.0.1";
        }

        // Hansı IP ilə müqayisə edəcəyik? (Statik yoxsa Dinamik?)
        String allowedOfficeIp;
        if (isStaticIp) {
            allowedOfficeIp = staticIpValue; // application.properties-dən gəlir
        } else {
            // Bazadakı OfficeConfig cədvəlindən (id=1) gəlir
            allowedOfficeIp = officeConfigRepository.findById(1L)
                    .map(OfficeConfig::getCurrentOfficeIp)
                    .orElse("127.0.0.1");
        }

        if (!allowedOfficeIp.equals(clientIp)) {
            throw new InvalidIpException(
                    "❌ Xəta: Siz ofis Wi-Fi şəbəkəsində deyilsiniz! (Sizin IP: " + clientIp + ")"
            );
        }
    }

    // 2. Dinamik IP rejimində ofis IP-sini yeniləmək üçün metod (HR və ya Script tərəfindən)
    public String updateDynamicOfficeIp(HttpServletRequest request, String providedSecret, String updatedBy) {
        if (!secretKey.equals(providedSecret)) {
            throw new IllegalArgumentException("Yanlış gizli açar (Secret Key)!");
        }

        String newIp = extractClientIp(request);
        if ("0:0:0:0:0:0:0:1".equals(newIp)) {
            newIp = "127.0.0.1";
        }

        OfficeConfig config = officeConfigRepository.findById(1L)
                .orElse(new OfficeConfig(1L, newIp, LocalDateTime.now(), updatedBy));

        config.setCurrentOfficeIp(newIp);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(updatedBy);

        officeConfigRepository.save(config);
        return newIp;
    }

    // Proxy və ya Cloud (Railway/Nginx) arxasında real IP-ni tapan köməkçi metod
    private String extractClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}