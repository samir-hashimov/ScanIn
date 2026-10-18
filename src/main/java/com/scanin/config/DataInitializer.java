package com.scanin.config;

import com.scanin.dao.entity.User;
import com.scanin.dao.repository.UserRepository;
import com.scanin.util.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .employeeCode("EMP-000")
                    .username("admin")
                    .password(passwordEncoder.encode("Admin123!"))
                    .fullName("Sistem Admin (HR)")
                    .position("HR Menecer")
                    .role(Role.ADMIN)
                    .shiftStartTime(LocalTime.of(9, 0))
                    .shiftEndTime(LocalTime.of(18, 0))
                    .build();
            userRepository.save(admin);
            log.info("🔑 İlk ADMIN hesabı yaradıldı -> username: admin | password: admin123");
        }
    }
}