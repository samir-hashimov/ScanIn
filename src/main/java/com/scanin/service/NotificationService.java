package com.scanin.service;

import com.scanin.dao.entity.Attendance;

public interface NotificationService {
    void sendCheckInNotification(Attendance attendance);
    void sendCheckOutNotification(Attendance attendance);
}