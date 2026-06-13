package ua.ivan.todo.tasks.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import ua.ivan.todo.tasks.common.exception.exceptions.AuthenticationException;
import ua.ivan.todo.tasks.security.dto.request.LoginRequest;
import ua.ivan.todo.tasks.security.dto.response.LoginResponse;
import ua.ivan.todo.tasks.security.utils.JwtUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldReturnJwtTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest(
            "nick@mail.com",
            "password123");

        UserDetails userDetails = User.builder()
            .username("nick@mail.com")
            .password("hashed-password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities());

        authentication.setAuthenticated(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(jwtUtils.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtUtils.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse actual = authService.login(request);

        assertThat(actual.accessToken()).isEqualTo("jwt-token");
        assertThat(actual.tokenType()).isEqualTo("Bearer");
        assertThat(actual.expiresInSeconds()).isEqualTo(3600L);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtils).generateToken(userDetails);
        verify(jwtUtils).getExpirationSeconds();
    }

    @Test
    void loginShouldAuthenticateWithRequestEmailAndPassword() {
        LoginRequest request = new LoginRequest(
            "nick@mail.com",
            "password123");

        UserDetails userDetails = User.builder()
            .username("nick@mail.com")
            .password("hashed-password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities());

        authentication.setAuthenticated(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(jwtUtils.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtUtils.getExpirationSeconds()).thenReturn(3600L);

        authService.login(request);

        verify(authenticationManager).authenticate(argThat(token -> token instanceof UsernamePasswordAuthenticationToken
            && "nick@mail.com".equals(token.getPrincipal())
            && "password123".equals(token.getCredentials())));
    }

    @Test
    void loginShouldThrowAuthenticationExceptionWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest(
            "nick@mail.com",
            "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("Invalid email or password");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void loginShouldThrowAuthenticationExceptionWhenUserIsDisabled() {
        LoginRequest request = new LoginRequest(
            "nick@mail.com",
            "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("User is disabled");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtUtils);
    }
}