package com.uit.backend_cinema.modules.auth.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.auth.domain.entity.AuthProvider;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true) // Cấu hình mặc định cho các hàm là chỉ đọc (tối ưu hiệu suất DB)
public class AuthService {
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Đăng ký tài khoản bằng email
    @Transactional // Bật ghi dữ liệu cho hàm này
    public User register(String email, String rawPassword, String name, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng", ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setName(name);
        user.setPhone(phone);
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(DEFAULT_ROLE);
        user.setIsDeleted(false);

        return userRepository.save(user);
    }

    // Đăng nhập bằng email/password
    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        "Email hoặc mật khẩu không chính xác",
                        ErrorCode.INVALID_CREDENTIALS
                ));

        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(
                    "Email hoặc mật khẩu không chính xác",
                    ErrorCode.INVALID_CREDENTIALS
            );
        }

        return user;
    }

    // Đăng nhập hoặc đăng ký bằng Google
    @Transactional // Bật ghi dữ liệu cho hàm này
    public User loginOrRegisterWithGoogle(String email, String name, String avatarUrl, String providerId) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isEmpty()) {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setAvatarUrl(avatarUrl);
            user.setProviderId(providerId);
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setRole(DEFAULT_ROLE);
            user.setIsDeleted(false);
        } else {
            user = existingUser.get();
            user.setName(name);
            user.setAvatarUrl(avatarUrl);
            user.setProviderId(providerId);
        }
        return userRepository.save(user);
    }

    // Đổi mật khẩu (quên mật khẩu)
    @Transactional
    public void changePassword(String email, String newPassword) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new BusinessException("Email không tồn tại", ErrorCode.INVALID_CREDENTIALS);
        }
        user.get().setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user.get());
    }

    // Đổi mật khẩu (khi đang đăng nhập)
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new BusinessException("Email không tồn tại", ErrorCode.INVALID_CREDENTIALS);
        }
        User user = userOpt.get();
        if (user.getPassword() == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Mật khẩu cũ không chính xác", ErrorCode.INVALID_CREDENTIALS);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
