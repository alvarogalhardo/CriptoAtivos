package com.criptoativos.auth;

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

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse body = UserResponse.from(userService.register(request));
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
    }
}
