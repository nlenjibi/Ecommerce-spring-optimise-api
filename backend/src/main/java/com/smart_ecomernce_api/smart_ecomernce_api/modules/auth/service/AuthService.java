package com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.service;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.dto.AuthResponse;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.dto.LoginRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.dto.RefreshTokenRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.dto.RegisterRequest;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.auth.entity.Auth;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    AuthResponse oauth2Login(User user, HttpServletRequest httpRequest);

    AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest);

    AuthResponse getCurrentUserSession(User user);

    AuthResponse getCurrentUserSessionById(Long userId);

    void logout(String refreshToken, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response);

    Auth validateRefreshToken(String refreshToken);

    void cleanupExpiredSessions();

    void changePassword(Long userId, String oldPassword, String newPassword);

    void lockAccount(Long userId, String reason);

    void unlockAccount(Long userId);
}