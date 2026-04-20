package com.uit.backend_cinema.common.sercurity;


import com.uit.backend_cinema.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Nonnull;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    //Công cụ để tra cứu thông tin user từ database
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        // Lấy token jwt
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Nếu không có hặc sai định dạng
        if (authHeader != null && !authHeader.startsWith("Bearer ")) {
            // Vẫn cho đi tếp (Có thể đang gọi API login/sign up)
            // Lát nữa vào trong sẽ bị hệ thống phân quyền (SecurityConfig) chặn lại nếu API đó
            // không cho khách vãng lai gọi
            filterChain.doFilter(request, response);
            return;
        }

        // Cắt bỏ chữ "Bearer"
        jwt = authHeader.substring(7);

        try {
            userEmail = jwtUtil.extractUsername(jwt);

            // Nếu có thông tin tuy nhiên chưa ghi nhận thông tin đăng nhập hiện tại
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Gọi xuống db để lấy thông tin của user này ra đối chiếu
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    // Nếu hợp lệ thì cấp cho user 1 vé thông hành
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                            null,
                            userDetails.getAuthorities()); // Các quyền của user

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            /*
            * Nếu token có vấn đề hoặc bị làm giả thì jwt util sẽ ném exception
            * Bỏ qua và không cho xác thực
            * Request coi như không có quyền và sẽ bị chặn lại ở lớp bảo vệ bên trong
            * */
            System.out.println("Lỗi xác thực JWT: " + e.getMessage());
        }

        // Cho phép request được đi tiếp (vào controller)
        filterChain.doFilter(request, response);
    }
}
