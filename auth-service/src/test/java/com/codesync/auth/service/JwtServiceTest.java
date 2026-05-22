package com.codesync.auth.service;

import com.codesync.auth.entity.Admin;
import com.codesync.auth.entity.User;
import com.codesync.auth.service.implementation.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link JwtService}.
 * No Spring context required – JwtService has no collaborators beyond
 * the injected {@code jwt.secret} value.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private static final String SECRET =
            "myVerySecretKeyThatIsLongEnoughForHS256Algorithm1234567890";

    private JwtService jwtService;
    private User testUser;
    private Admin testAdmin;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);

        testUser = new User();
        testUser.setUserId(1);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
        testUser.setProvider("LOCAL");

        testAdmin = new Admin();
        testAdmin.setAdminId(10);
        testAdmin.setUsername("admin_user");
    }

    // ── generateToken (User) ──────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken(User)")
    class GenerateUserTokenTests {

        @Test
        @DisplayName("returns a non-null, non-blank JWT string")
        void generateToken_notBlank() {
            String token = jwtService.generateToken(testUser);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("token has three dot-separated segments (header.payload.signature)")
        void generateToken_hasThreeSegments() {
            String token = jwtService.generateToken(testUser);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("extractUserName returns the user's username")
        void generateToken_extractUsername_correct() {
            String token = jwtService.generateToken(testUser);
            assertThat(jwtService.extractUserName(token)).isEqualTo("testuser");
        }

        @Test
        @DisplayName("extractRole returns USER role")
        void generateToken_extractRole_user() {
            String token = jwtService.generateToken(testUser);
            assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        }

        @Test
        @DisplayName("DEVELOPER role is preserved in token")
        void generateToken_developerRole_preserved() {
            testUser.setRole("DEVELOPER");
            String token = jwtService.generateToken(testUser);
            assertThat(jwtService.extractRole(token)).isEqualTo("DEVELOPER");
        }

        @Test
        @DisplayName("different users produce different tokens")
        void generateToken_differentUsersProduceDifferentTokens() {
            String token1 = jwtService.generateToken(testUser);

            User user2 = new User();
            user2.setUserId(2);
            user2.setUserName("anotheruser");
            user2.setEmail("other@example.com");
            user2.setRole("USER");
            user2.setProvider("LOCAL");
            String token2 = jwtService.generateToken(user2);

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ── generateAdminToken ────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateAdminToken(Admin)")
    class GenerateAdminTokenTests {

        @Test
        @DisplayName("returns a non-null, non-blank JWT string")
        void generateAdminToken_notBlank() {
            String token = jwtService.generateAdminToken(testAdmin);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("extractUserName returns admin's username")
        void generateAdminToken_extractUsername() {
            String token = jwtService.generateAdminToken(testAdmin);
            assertThat(jwtService.extractUsername(token)).isEqualTo("admin_user");
        }

        @Test
        @DisplayName("extractRole returns ADMIN for admin token")
        void generateAdminToken_extractRole_admin() {
            String token = jwtService.generateAdminToken(testAdmin);
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken(token, UserDetails)")
    class ValidateTokenTests {

        @Test
        @DisplayName("valid token with matching username returns true")
        void validateToken_validToken_returnsTrue() {
            String token = jwtService.generateToken(testUser);
            UserDetails ud = org.springframework.security.core.userdetails.User
                    .withUsername("testuser").password("pass").roles("USER").build();

            assertThat(jwtService.validateToken(token, ud)).isTrue();
        }

        @Test
        @DisplayName("token with mismatched username returns false")
        void validateToken_mismatchedUsername_returnsFalse() {
            String token = jwtService.generateToken(testUser);
            UserDetails ud = org.springframework.security.core.userdetails.User
                    .withUsername("WRONG_USER").password("pass").roles("USER").build();

            assertThat(jwtService.validateToken(token, ud)).isFalse();
        }
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTokenValid(token, username)")
    class IsTokenValidTests {

        @Test
        @DisplayName("returns true when username matches and token is not expired")
        void isTokenValid_matching_returnsTrue() {
            String token = jwtService.generateToken(testUser);
            assertThat(jwtService.isTokenValid(token, "testuser")).isTrue();
        }

        @Test
        @DisplayName("returns false when username does not match")
        void isTokenValid_mismatch_returnsFalse() {
            String token = jwtService.generateToken(testUser);
            assertThat(jwtService.isTokenValid(token, "wronguser")).isFalse();
        }
    }

    // ── extractUsername / extractUserName ────────────────────────────────────

    @Nested
    @DisplayName("extractUsername / extractUserName")
    class ExtractUsernameTests {

        @Test
        @DisplayName("extractUsername and extractUserName return identical values")
        void extractMethods_returnSameValue() {
            String token = jwtService.generateToken(testUser);
            assertThat(jwtService.extractUsername(token))
                    .isEqualTo(jwtService.extractUserName(token));
        }
    }
}
