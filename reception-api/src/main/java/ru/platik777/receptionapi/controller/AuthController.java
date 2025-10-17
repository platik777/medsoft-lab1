package ru.platik777.receptionapi.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.platik777.receptionapi.entity.User;
import ru.platik777.receptionapi.repository.UserRepository;
import ru.platik777.receptionapi.security.JwtUtil;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(new ErrorResponse("Неверное имя пользователя или пароль"));
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        log.info("Пользователь {} успешно вошел в систему", user.getUsername());

        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getFullName(), user.getRole()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Пользователь уже существует"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole("ROLE_RECEPTIONIST");

        userRepository.save(user);

        log.info("Зарегистрирован новый пользователь: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getFullName(), user.getRole()));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (jwtUtil.isTokenValid(token, username)) {
                return ResponseEntity.ok(new MessageResponse("Token is valid"));
            }
        } catch (Exception e) {
            log.error("Token validation failed", e);
        }
        return ResponseEntity.status(401).body(new ErrorResponse("Invalid token"));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String fullName;
    }

    public record LoginResponse(String token, String username, String fullName, String role) {}

    public record ErrorResponse(String error) {}

    public record MessageResponse(String message) {}
}