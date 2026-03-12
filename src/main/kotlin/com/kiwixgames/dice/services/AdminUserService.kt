package com.kiwixgames.dice.services

import com.kiwixgames.dice.domain.dtos.admin.AdminAdjustBalanceRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminCreateUserRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminUpdateUserRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminUserResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AdminUserService {
    fun list(search: String?, pageable: Pageable): Page<AdminUserResponse>
    fun get(id: Long): AdminUserResponse
    fun create(req: AdminCreateUserRequest): AdminUserResponse
    fun update(id: Long, req: AdminUpdateUserRequest): AdminUserResponse
    fun delete(id: Long)
    fun getBalance(id: Long): AdminUserResponse
    fun adjustBalance(id: Long, req: AdminAdjustBalanceRequest): AdminUserResponse
    fun setBalance(id: Long, req: AdminAdjustBalanceRequest): AdminUserResponse
}
