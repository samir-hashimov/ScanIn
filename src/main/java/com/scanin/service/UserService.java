package com.scanin.service;

import com.scanin.dao.entity.User;
import com.scanin.dao.repository.UserRepository;
import com.scanin.dto.request.ActivateAccountRequest;
import com.scanin.dto.request.CreateUserRequest;
import com.scanin.dto.response.UserResponse;
import com.scanin.exception.ResourceNotFoundException;
import com.scanin.mapper.UserMapper;
import com.scanin.util.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // 1. HR sadəcə Ad-Soyad və Vəzifə yazır, sistem EMP-001 kodu verir (username və parol boş qalır):
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        User user = userMapper.toEntity(request);

        // Avtomatik EMP-001, EMP-002 kodu yaradırıq:
        user.setEmployeeCode(generateNextEmployeeCode());

        if (user.getShiftStartTime() == null) {
            user.setShiftStartTime(LocalTime.of(9, 0));
        }
        if (user.getShiftEndTime() == null) {
            user.setShiftEndTime(LocalTime.of(18, 0));
        }
        if (!user.getShiftEndTime().isAfter(user.getShiftStartTime())) {
            throw new IllegalArgumentException("İşdən çıxış saatı başlama saatından sonra olmalıdır!");
        }
        if (user.getRole() == null) {
            user.setRole(Role.EMPLOYEE);
        }

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    // 2. YENİ: İşçi ilk dəfə QR oxudanda EMP-001 kodunu yazır, öz istədiyi username ("nurka") və parolu təyin edir:
    @Transactional
    public User activateEmployeeAccount(ActivateAccountRequest request) {
        // İşçini EMP koduna görə tapırıq (məs: "emp-001" yazsa belə böyük hərflə "EMP-001" axtarır)
        User user = userRepository.findByEmployeeCode(request.getEmployeeCode().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "❌ Belə bir İşçi Kodu tapılmadı: " + request.getEmployeeCode() + ". Zəhmət olmasa HR ilə dəqiqləşdirin."
                ));

        // Əgər bu hesab artıq əvvəlcədən aktivləşdirilibsə (başqası onun kodunu yazıb dəyişə bilməsin!):
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            throw new IllegalArgumentException(
                    "⚠️ Bu işçi kodu (" + user.getEmployeeCode() + ") artıq aktivləşdirilib! Öz username və şifrənizlə Login olun."
            );
        }

        // Seçdiyi username (məs: "nurka") başqa işçi tərəfindən tutulmayıb ki?
        String desiredUsername = request.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsername(desiredUsername)) {
            throw new IllegalArgumentException(
                    "❌ \"" + desiredUsername + "\" istifadəçi adı artıq başqası tərəfindən istifadə olunur. Başqa ad seçin!"
            );
        }

        // İşçinin öz seçdiyi username və şifrəni (hash-lənmiş) yadda saxlayırıq:
        user.setUsername(desiredUsername);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return userRepository.save(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı: " + username));
    }

    private String generateNextEmployeeCode() {
        long nextNumber = userRepository.findTopByOrderByIdDesc()
                .map(lastUser -> lastUser.getId() + 1)
                .orElse(1L);
        return String.format("EMP-%03d", nextNumber);
    }
}