package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.controller;

import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.entity.User;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/v1/users/auth/register - User Registration")
    class Register {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterUser() throws Exception {
            String requestBody = """
                {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "Password123!",
                    "firstName": "John",
                    "lastName": "Doe",
                    "phoneNumber": "1234567890"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        @DisplayName("Should fail to register with duplicate email")
        void shouldFailWithDuplicateEmail() throws Exception {
            // First registration
            String requestBody1 = """
                {
                    "email": "test@example.com",
                    "username": "testuser1",
                    "password": "Password123!",
                    "firstName": "John",
                    "lastName": "Doe",
                    "phoneNumber": "1234567890"
                }
                """;
            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody1))
                    .andExpect(status().isCreated());

            // Second registration with same email
            String requestBody2 = """
                {
                    "email": "test@example.com",
                    "username": "testuser2",
                    "password": "Password123!",
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "phoneNumber": "0987654321"
                }
                """;
            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody2))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to register with duplicate username")
        void shouldFailWithDuplicateUsername() throws Exception {
            String requestBody1 = """
                {
                    "email": "test1@example.com",
                    "username": "testuser",
                    "password": "Password123!",
                    "firstName": "John",
                    "lastName": "Doe"
                }
                """;
            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody1))
                    .andExpect(status().isCreated());

            String requestBody2 = """
                {
                    "email": "test2@example.com",
                    "username": "testuser",
                    "password": "Password123!",
                    "firstName": "Jane",
                    "lastName": "Doe"
                }
                """;
            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody2))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to register with invalid email")
        void shouldFailWithInvalidEmail() throws Exception {
            String requestBody = """
                {
                    "email": "invalid-email",
                    "username": "testuser",
                    "password": "Password123!",
                    "firstName": "John",
                    "lastName": "Doe"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to register with weak password")
        void shouldFailWithWeakPassword() throws Exception {
            String requestBody = """
                {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "weak",
                    "firstName": "John",
                    "lastName": "Doe"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to register with missing required fields")
        void shouldFailWithMissingFields() throws Exception {
            String requestBody = """
                {
                    "email": "test@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/auth/login - User Login")
    class Login {

        @BeforeEach
        void createUser() throws Exception {
            String requestBody = """
                {
                    "email": "login@example.com",
                    "username": "loginuser",
                    "password": "Password123!",
                    "firstName": "John",
                    "lastName": "Doe",
                    "phoneNumber": "1234567890"
                }
                """;
            mockMvc.perform(post("/api/v1/users/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody));
        }

        @Test
        @DisplayName("Should login successfully with email")
        void shouldLoginWithEmail() throws Exception {
            String requestBody = """
                {
                    "usernameOrEmail": "login@example.com",
                    "password": "Password123!"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("login@example.com"))
                    .andExpect(jsonPath("$.data.sessionToken").exists());
        }

        @Test
        @DisplayName("Should login successfully with username")
        void shouldLoginWithUsername() throws Exception {
            String requestBody = """
                {
                    "usernameOrEmail": "loginuser",
                    "password": "Password123!"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("loginuser"));
        }

        @Test
        @DisplayName("Should fail login with wrong password")
        void shouldFailWithWrongPassword() throws Exception {
            String requestBody = """
                {
                    "usernameOrEmail": "login@example.com",
                    "password": "WrongPassword123!"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should fail login with non-existent user")
        void shouldFailWithNonExistentUser() throws Exception {
            String requestBody = """
                {
                    "usernameOrEmail": "nonexistent@example.com",
                    "password": "Password123!"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should fail login with missing credentials")
        void shouldFailWithMissingCredentials() throws Exception {
            String requestBody = """
                {
                    "usernameOrEmail": "login@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/users/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/auth/logout - User Logout")
    class Logout {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogout() throws Exception {
            mockMvc.perform(post("/api/v1/users/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/auth/me - Get Current User")
    class GetCurrentUser {

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/users/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
