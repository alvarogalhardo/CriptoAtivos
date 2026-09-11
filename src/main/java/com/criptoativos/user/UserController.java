package com.criptoativos.user;

import com.criptoativos.common.SecurityUtils;
import com.criptoativos.user.dto.UpdateProfileRequest;
import com.criptoativos.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(userService.requireById(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(userService.rename(SecurityUtils.currentUserId(), request.name()));
    }
}
