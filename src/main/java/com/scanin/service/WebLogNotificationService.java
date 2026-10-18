package com.scanin.service;

import com.scanin.dao.entity.Attendance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WebLogNotificationService implements NotificationService {

    @Override
    public void sendCheckInNotification(Attendance attendance) {
        log.info("🟢 [BİLDİRİŞ - GİRİŞ] {} ({}) saat {}-da işə gəldi. Status: {}",
                attendance.getUser().getFullName(),
                attendance.getUser().getPosition(),
                attendance.getCheckInTime().toLocalTime(),
                attendance.getArrivalStatus());
    }

    @Override
    public void sendCheckOutNotification(Attendance attendance) {
        log.info("🔴 [BİLDİRİŞ - ÇIXIŞ] {} ({}) saat {}-da işdən çıxdı. Ümumi iş: {} dəq.",
                attendance.getUser().getFullName(),
                attendance.getUser().getPosition(),
                attendance.getCheckOutTime().toLocalTime(),
                attendance.getTotalWorkedMinutes());
    }
}