package ua.ivan.todo.tasks.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ua.ivan.todo.tasks.common.exception.exceptions.AuthenticationException;
import ua.ivan.todo.tasks.security.dto.request.LoginRequest;
import ua.ivan.todo.tasks.security.dto.response.LoginResponse;
import ua.ivan.todo.tasks.security.utils.JwtUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()));
        } catch (BadCredentialsException exception) {
            throw new AuthenticationException("Invalid email or password");
        } catch (DisabledException exception) {
            throw new AuthenticationException("User is disabled");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        return new LoginResponse(
            token,
            "Bearer",
            jwtUtils.getExpirationSeconds());
    }
}