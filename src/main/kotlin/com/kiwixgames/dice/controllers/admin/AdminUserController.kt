package com.kiwixgames.dice.controllers.admin

import com.kiwixgames.dice.domain.dtos.admin.AdminAdjustBalanceRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminCreateUserRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminUpdateUserRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminUserResponse
import com.kiwixgames.dice.services.AdminUserService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin User", description = "Admin User Operations")
class AdminUserController(
    private val adminUserService: AdminUserService
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) search: String?,
        pageable: Pageable
    ): ResponseEntity<Page<AdminUserResponse>> =
        ResponseEntity.ok(adminUserService.list(search, pageable))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<AdminUserResponse> =
        ResponseEntity.ok(adminUserService.get(id))

    @PostMapping
    fun create(@Valid @RequestBody req: AdminCreateUserRequest): ResponseEntity<AdminUserResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(adminUserService.create(req))

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: AdminUpdateUserRequest
    ): ResponseEntity<AdminUserResponse> =
        ResponseEntity.ok(adminUserService.update(id, req))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        adminUserService.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/wallet")
    fun getBalance(@PathVariable id: Long): ResponseEntity<AdminUserResponse> =
        ResponseEntity.ok(adminUserService.getBalance(id))

    @PostMapping("/{id}/wallet/adjust")
    fun adjustBalance(
        @PathVariable id: Long,
        @Valid @RequestBody req: AdminAdjustBalanceRequest
    ): ResponseEntity<AdminUserResponse> =
        ResponseEntity.ok(adminUserService.adjustBalance(id, req))

    @PutMapping("/{id}/wallet")
    fun setBalance(
        @PathVariable id: Long,
        @Valid @RequestBody req: AdminAdjustBalanceRequest
    ): ResponseEntity<AdminUserResponse> =
        ResponseEntity.ok(adminUserService.setBalance(id, req))
}
