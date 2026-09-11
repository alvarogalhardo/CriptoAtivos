package com.criptoativos.admin;

import com.criptoativos.admin.dto.AdminDtos.AdminUserResponse;
import com.criptoativos.admin.dto.AdminDtos.ChangeRoleRequest;
import com.criptoativos.common.SecurityUtils;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Class-level {@code @PreAuthorize} so no method here can be left unguarded by accident. */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @PageableAsQueryParam
    public Page<AdminUserResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    @Parameter(hidden = true)
                    Pageable pageable) {
        return adminUserService.list(search, pageable);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable UUID id) {
        return adminUserService.view(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean force) {
        adminUserService.delete(SecurityUtils.currentUserId(), id, force);
    }

    @PatchMapping("/{id}/role")
    public AdminUserResponse changeRole(
            @PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest request) {
        return adminUserService.changeRole(SecurityUtils.currentUserId(), id, request.role());
    }
}
