package com.uit.backend_cinema.common.sercurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Bật tính năng phân quyền nhanh cho từng hàm bằng @PreAuthorize
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    // Inject service vào
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, 
                          UserDetailsService userDetailsService,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Chịu trách nhiệm gọi provider để xử lý yêu cầu đăng nhập
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF (Cross-Site Request Forgery) vì chúng ta dùng JWT (Stateless), không dùng Cookie
                .csrf(AbstractHttpConfigurer::disable)

                // Cấu hình CORS (Cho phép Mobile domain khác gọi API)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Phân quyền cho từng URL cụ thể
                .authorizeHttpRequests(auth -> auth
                        // Khu vực công cộng: Ai cũng vào được (Đăng nhập, Đăng ký, Quên mật khẩu)
                        .requestMatchers("/api/v1/auth/change-password").authenticated() // Yêu cầu JWT hợp lệ
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Cho phép truy cập Swagger UI tài liệu API
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Khu vực công cộng: Khách vãng lai có thể xem danh sách phim
                        .requestMatchers("/api/v1/public/**").permitAll()

                        // Khu vực cấm: Chỉ ADMIN mới được vào Quản lý phim (Thêm/Sửa/Xóa)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Cho phép truy cập route báo lỗi nội bộ của Spring Boot (để tránh bị 403 giả)
                        .requestMatchers("/error").permitAll()

                        // Tất cả các request khác bắt buộc phải có JWT hợp lệ
                        .anyRequest().authenticated()
                )

                // Cấu hình custom exception handler để trả về chuẩn ErrorResponse
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint) // Xử lý lỗi 401
                        .accessDeniedHandler(accessDeniedHandler)           // Xử lý lỗi 403
                )

                // Khai báo hệ thống không lưu trạng thái đăng nhập (Stateless)
                // Mỗi lần gọi API là 1 lần phải trình JWT ra
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Chỉ định provider
                .authenticationProvider(authenticationProvider())

                // Thêm 1 lớp filter request ở phía trước
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CẤU HÌNH CORS (Cross-Origin Resource Sharing)
    // Để App Mobile có thể gọi được API mà không bị trình duyệt chặn
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Trong thực tế nên để domain cụ thể (VD: https://myapp.com)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Áp dụng luật này cho toàn bộ API
        return source;
    }
}
