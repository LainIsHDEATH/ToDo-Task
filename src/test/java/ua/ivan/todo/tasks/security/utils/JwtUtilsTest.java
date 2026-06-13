package ua.ivan.todo.tasks.security.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "expirationMinutes", 60L);
    }

    @Test
    void generateTokenShouldReturnEncodedToken() {
        UserDetails userDetails = User.builder()
            .username("user@mail.com")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();

        Jwt encodedJwt = Jwt.withTokenValue("jwt-token")
            .header("alg", "HS256")
            .subject("user@mail.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("authorities", List.of("ROLE_USER"))
            .build();

        when(jwtEncoder.encode(org.mockito.ArgumentMatchers.any(JwtEncoderParameters.class)))
            .thenReturn(encodedJwt);

        String actual = jwtUtils.generateToken(userDetails);

        assertThat(actual).isEqualTo("jwt-token");
    }

    @Test
    void generateTokenShouldIncludeUsernameAndAuthorities() {
        UserDetails userDetails = User.builder()
            .username("admin@mail.com")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .build();

        Jwt encodedJwt = Jwt.withTokenValue("jwt-token")
            .header("alg", "HS256")
            .subject("admin@mail.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("authorities", List.of("ROLE_ADMIN"))
            .build();

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
            ArgumentCaptor.forClass(JwtEncoderParameters.class);

        when(jwtEncoder.encode(parametersCaptor.capture())).thenReturn(encodedJwt);

        jwtUtils.generateToken(userDetails);

        JwtEncoderParameters parameters = parametersCaptor.getValue();

        assertThat(parameters.getClaims().getSubject()).isEqualTo("admin@mail.com");
        assertThat(parameters.getClaims().getClaimAsStringList("authorities")).containsExactly("ROLE_ADMIN");
        assertThat(parameters.getClaims().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void extractUsernameShouldReturnJwtSubject() {
        Jwt jwt = jwt("token",
                "user@mail.com",
                Instant.now(),
                Instant.now().plusSeconds(3600));

        when(jwtDecoder.decode("token")).thenReturn(jwt);

        String actual = jwtUtils.extractUsername("token");

        assertThat(actual).isEqualTo("user@mail.com");
    }

    @Test
    void isTokenValidShouldReturnTrueForMatchingUserAndNotExpiredToken() {
        UserDetails userDetails = User.builder()
            .username("user@mail.com")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();

        Jwt jwt = jwt("token",
                "user@mail.com",
                Instant.now(),
                Instant.now().plusSeconds(3600));

        when(jwtDecoder.decode("token")).thenReturn(jwt);

        boolean actual = jwtUtils.isTokenValid("token", userDetails);

        assertThat(actual).isTrue();
    }

    @Test
    void isTokenValidShouldReturnFalseForExpiredToken() {
        UserDetails userDetails = User.builder()
            .username("user@mail.com")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();

        Jwt jwt = jwt("token",
                "user@mail.com",
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(60));

        when(jwtDecoder.decode("token")).thenReturn(jwt);

        boolean actual = jwtUtils.isTokenValid("token", userDetails);

        assertThat(actual).isFalse();
    }

    @Test
    void getExpirationSecondsShouldReturnExpirationInSeconds() {
        long actual = jwtUtils.getExpirationSeconds();

        assertThat(actual).isEqualTo(3600);
    }

    private Jwt jwt(String token, String subject, Instant issuedAt, Instant expiresAt) {
        return new Jwt(
            token,
            issuedAt,
            expiresAt,
            Map.of("alg", "HS256"),
            Map.of("sub", subject));
    }
}