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
        User user = userRepository.findByUsername(request.username()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(401).body(new ErrorResponse("Неверное имя пользователя или пароль"));
        }

        String token = jwtUtil.generateToken(user.getUsername());

        log.info("Пользователь {} успешно вошел в систему", user.getUsername());

        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getFullName()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Пользователь уже существует"));
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());

        userRepository.save(user);

        log.info("Зарегистрирован новый пользователь: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getFullName()));
    }

    public record LoginRequest(String username, String password) {}

    public record RegisterRequest (String username, String password, String fullName) {}

    public record LoginResponse(String token, String username, String fullName) {}

    public record ErrorResponse(String error) {}
}