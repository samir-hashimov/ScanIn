package com.scanin.util;

public enum DepartureStatus {
    WORKING,          // Səhər gəlib, hazırda ofisdədir (hələ çıxış etməyib)
    COMPLETED,        // İşini bitirib normal çıxış edib
    EARLY_LEAVE,      // 10 dəqiqə içində və ya vaxtından tez icazə/səbəb yazıb çıxıb
    FORGOT_CHECKOUT   // Çıxanda QR oxutmağı unudub (Gecə 23:59-da sistem bağlayıb)
}