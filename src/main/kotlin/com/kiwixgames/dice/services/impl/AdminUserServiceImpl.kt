package com.kiwixgames.dice.services.impl

import com.kiwixgames.dice.domain.dtos.admin.AdminAdjustBalanceRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminCreateUserRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminUpdateUserRequest
import com.kiwixgames.dice.domain.dtos.admin.AdminUserResponse
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.entities.Wallet
import com.kiwixgames.dice.domain.entities.WalletTxn
import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.domain.enums.WalletTxnType
import com.kiwixgames.dice.mappers.AdminUserMapper
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.repositories.UserRepository
import com.kiwixgames.dice.repositories.WalletRepository
import com.kiwixgames.dice.repositories.WalletTxnRepository
import com.kiwixgames.dice.services.AdminUserService
import com.kiwixgames.dice.services.GamePlayService
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminUserServiceImpl(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
    private val walletTxnRepository: WalletTxnRepository,
    private val passwordEncoder: PasswordEncoder,
    private val gameRepository: GameRepository,
    private val gamePlayService: GamePlayService
) : AdminUserService {

    private val log = LoggerFactory.getLogger(AdminUserServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun list(search: String?, pageable: Pageable): Page<AdminUserResponse> {
        val page = if (search.isNullOrBlank()) {
            userRepository.findAll(pageable)
        } else {
            userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search, search, pageable
            )
        }
        return page.map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    override fun get(id: Long): AdminUserResponse {
        return toResponse(findUser(id))
    }

    @Transactional
    override fun create(req: AdminCreateUserRequest): AdminUserResponse {
        if (userRepository.existsByUsername(req.username)) {
            throw IllegalStateException("Username '${req.username}' is already taken")
        }
        if (userRepository.existsByEmail(req.email)) {
            throw IllegalStateException("Email '${req.email}' is already taken")
        }

        val user = userRepository.save(
            User(
                username = req.username,
                email = req.email,
                password = passwordEncoder.encode(req.password)!!,
                firstName = req.firstName,
                lastName = req.lastName,
                role = req.role
            )
        )

        if (req.balanceGold > 0) {
            walletRepository.save(Wallet(user = user, balanceGold = req.balanceGold))
            walletTxnRepository.save(
                WalletTxn(
                    user = user,
                    amount = req.balanceGold,
                    type = WalletTxnType.ADJUSTMENT,
                    note = "Initial balance set by admin"
                )
            )
        }

        return toResponse(user)
    }

    @Transactional
    override fun update(id: Long, req: AdminUpdateUserRequest): AdminUserResponse {
        val user = findUser(id)

        req.username?.let {
            if (it != user.username && userRepository.existsByUsername(it)) {
                throw IllegalStateException("Username '$it' is already taken")
            }
            user.username = it
        }
        req.email?.let {
            if (it != user.email && userRepository.existsByEmail(it)) {
                throw IllegalStateException("Email '$it' is already taken")
            }
            user.email = it
        }
        req.password?.let { user.password = passwordEncoder.encode(it)!! }
        req.firstName?.let { user.firstName = it }
        req.lastName?.let { user.lastName = it }
        req.role?.let { user.role = it }

        val wasActive = user.isActive
        req.isActive?.let { user.isActive = it }

        userRepository.save(user)

        if (wasActive && !user.isActive) {
            forfeitActiveGames(user)
        }

        return toResponse(user)
    }

    @Transactional
    override fun delete(id: Long) {
        val user = findUser(id)
        user.isActive = false
        userRepository.save(user)
        forfeitActiveGames(user)
    }

    @Transactional(readOnly = true)
    override fun getBalance(id: Long): AdminUserResponse {
        return toResponse(findUser(id))
    }

    @Transactional
    override fun adjustBalance(id: Long, req: AdminAdjustBalanceRequest): AdminUserResponse {
        val user = findUser(id)
        val wallet = getOrCreateWallet(user)

        val newBalance = wallet.balanceGold + req.amount
        if (newBalance < 0) {
            throw IllegalStateException(
                "Insufficient balance. Current: ${wallet.balanceGold}, adjustment: ${req.amount}"
            )
        }

        wallet.balanceGold = newBalance
        walletRepository.save(wallet)

        walletTxnRepository.save(
            WalletTxn(
                user = user,
                amount = req.amount,
                type = WalletTxnType.ADJUSTMENT,
                note = req.note ?: "Admin balance adjustment"
            )
        )

        return toResponse(user)
    }

    @Transactional
    override fun setBalance(id: Long, req: AdminAdjustBalanceRequest): AdminUserResponse {
        val user = findUser(id)
        val wallet = getOrCreateWallet(user)

        if (req.amount < 0) {
            throw IllegalStateException("Balance cannot be negative")
        }

        val diff = req.amount - wallet.balanceGold
        wallet.balanceGold = req.amount
        walletRepository.save(wallet)

        walletTxnRepository.save(
            WalletTxn(
                user = user,
                amount = diff,
                type = WalletTxnType.ADJUSTMENT,
                note = req.note ?: "Admin set balance to ${req.amount}"
            )
        )

        return toResponse(user)
    }

    private fun forfeitActiveGames(user: User) {
        val activeGames = gameRepository.findAllByStatusAndUserId(GameStatus.IN_PROGRESS, user.id!!)
        for (game in activeGames) {
            try {
                gamePlayService.forfeit(game.id!!, user, "DEACTIVATED")
                log.info("Forfeited gameId=${game.id} for deactivated userId=${user.id}")
            } catch (e: Exception) {
                log.error("Failed to forfeit gameId=${game.id} for deactivated userId=${user.id}: ${e.message}", e)
            }
        }
    }

    private fun findUser(id: Long): User =
        userRepository.findById(id).orElseThrow {
            EntityNotFoundException("User not found: $id")
        }

    private fun getOrCreateWallet(user: User): Wallet =
        walletRepository.findByUserId(user.id!!)
            ?: walletRepository.save(Wallet(user = user, balanceGold = 0))

    private fun toResponse(user: User): AdminUserResponse {
        val balance = walletRepository.findByUserId(user.id!!)?.balanceGold ?: 0
        return AdminUserMapper.toDto(user, balance)
    }
}
