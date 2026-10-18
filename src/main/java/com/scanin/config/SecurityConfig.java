package com.scanin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // JWT istifadə etdiyimiz üçün serverdə Session saxlamırıq (STATELESS):
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Login endpoint-i və HTML/JS səhifələri hamıya açıqdır:
                        .requestMatchers("/api/auth/**", "/", "/index.html", "/hr.html", "/static/**", "/*.js", "/*.css").permitAll()
                        // 2. Swagger açıq olsun (test üçün):
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // 3. Dinamik IP yeniləmə scripti öz gizli şifrəsi (secret) ilə işlədiyi üçün açıq buraxırıq:
                        .requestMatchers("/api/hr/office-ip/update").permitAll()
                        // 4. HR və Admin endpoint-lərinə YALNIZ HR və ADMIN girə bilər!
                        .requestMatchers("/api/hr/**").hasAnyRole("HR", "ADMIN")
                        // 5. QR Kod oxutma (/api/attendance/scan) üçün mütləq login olmuş (Token-li) işçi olmalıdır:
                        .requestMatchers("/api/attendance/**").hasAnyRole("EMPLOYEE", "HR", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}