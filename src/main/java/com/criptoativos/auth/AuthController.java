package com.criptoativos.auth;

import com.criptoativos.auth.dto.LoginRequest;
import com.criptoativos.auth.dto.LoginResponse;
import com.criptoativos.user.UserService;
import com.criptoativos.user.dto.RegisterRequest;
import com.criptoativos.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse body = UserResponse.from(userService.register(request));
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
    }
}
