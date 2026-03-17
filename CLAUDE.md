# Dice Game — Backend (Kotlin / Spring Boot)

2 kişilik Farkle zar oyunu backend'i. Oyuncular masa oluşturur, rakip katılınca oyun başlar. Sırayla zar atılır, puanlı zarlar tutulur, puan bankalanır veya tekrar atılır. Hedef skora ilk ulaşan kazanır.

## Tech Stack

- **Kotlin** + **Spring Boot 4.0.1** + **Java 21**
- **PostgreSQL** (port 5433) + **Flyway** migrations (`src/main/resources/db/migration/`)
- **Redis** (port 6379) — token blacklist
- **JWT** (HS256) — access token (15dk), refresh token (7 gün), role claim içerir
- **STOMP over WebSocket** — `/ws` endpoint, `/topic/games/{gameId}` broadcast
- **Gradle** build system

## Proje Yapısı

```
src/main/kotlin/com/kiwixgames/dice/
├── config/           # CORS, Security, WebSocket, Redis, Swagger, Jackson
├── controllers/      # REST endpoint'leri (Auth, Game, Table, Wallet, admin/)
├── domain/
│   ├── entities/     # JPA entity'leri (User, Game, GameTable, Wallet, WagerLock, WalletTxn)
│   ├── enums/        # GameStatus, TableStatus, Role, TurnPhase, BadgeTier, vb.
│   ├── dtos/         # Request/Response DTO'ları (admin/, auth/, game/, table/, wallet/)
│   ├── model/game/   # GameState, RolledDie (in-memory oyun durumu)
│   └── data/         # TableRules
├── exceptions/       # Custom exception'lar
├── jobs/             # GameTimeoutJob (scheduled tasks)
├── listeners/        # WebSocketDisconnectListener
├── mappers/          # Entity→DTO mapper'ları
├── repositories/     # Spring Data JPA repository'leri
├── security/         # JWT filter, UserDetails, CurrentUserProvider, RateLimiting
└── services/
    ├── impl/         # Service implementasyonları
    ├── game/         # Scoring engine (Farkle kuralları, DFS + memoization)
    │   └── rules/    # Pluggable ScoringRule implementasyonları
    └── rules/        # RulesEngine (masa kural validasyonu)
```

## Oyun Akışı

1. **Masa oluştur** → `POST /api/v1/tables` (stake + targetScore belirlenir, wager lock yapılır)
2. **Rakip katılır** → `POST /api/v1/tables/{id}/join` (wager lock + oyun başlar)
3. **Oyun döngüsü** (WebSocket + REST):
   - `roll` → zarları at (bust olursa sıra değişir)
   - `keep` → puanlı zarları tut (turn score birikir)
   - `bank` → puanı kaydet + sıra değişir (hedef skora ulaşırsa oyun biter)
   - `forfeit` → pes et
4. **Oyun bitince** → kazanana 2x stake ödenir, masa FINISHED olur

## Tur Fazları (TurnPhase)

- `MUST_ROLL` → Zar atmalı
- `MUST_KEEP_OR_BUST` → Puanlı zar tutmalı (yoksa bust)
- `CAN_ROLL_OR_BANK` → Zar atabilir veya bankalayabilir

## Farkle Skorlama Kuralları

| Kural | Puan |
|-------|------|
| Tek 1 | 100 |
| Tek 5 | 50 |
| 3-of-a-kind | 1→1000, N(2-6)→N×100 |
| 4-of-a-kind | 3-of-a-kind × 2 |
| 5-of-a-kind | 3-of-a-kind × 4 |
| 6-of-a-kind | 3-of-a-kind × 8 |
| Straight (1-2-3-4-5-6) | 3000 |
| 3 Pair (4+2 dahil) | 1500 |
| 2 Triplet | 2500 |

Skorlama motoru: `Kcd2ScoringMax` — DFS + memoization ile optimal kombinasyonu bulur. Kurallar pluggable `ScoringRule` interface'i ile modüler.

## "Hot Dice"

Tüm 6 zar bir turda tutulursa, 6 zar sıfırlanıp tekrar atılabilir (tur devam eder).

## REST Endpoint'leri

### Auth (`/api/v1/auth`)
| Method | Path | Auth | Açıklama |
|--------|------|------|----------|
| POST | `/login` | Hayır | Giriş yap |
| POST | `/register` | Hayır | Kayıt ol |
| POST | `/refresh` | Hayır | Token yenile (refresh cookie) |
| POST | `/logout` | Evet | Çıkış yap |

### Game (`/api/v1/games`)
| Method | Path | Auth | Açıklama |
|--------|------|------|----------|
| GET | `/{gameId}` | Evet | Oyun durumu |
| POST | `/{gameId}/roll` | Evet | Zar at |
| POST | `/{gameId}/keep` | Evet | Zar tut `{ "slots": [0,2,4] }` |
| POST | `/{gameId}/bank` | Evet | Puanı bankala |
| POST | `/{gameId}/forfeit` | Evet | Pes et |
| POST | `/{gameId}/ping` | Evet | Presence bildirimi (10s aralıkla) |

### Table (`/api/v1/tables`)
| Method | Path | Auth | Açıklama |
|--------|------|------|----------|
| POST | `/` | Evet | Masa oluştur |
| GET | `/waiting` | Hayır | Bekleyen masaları listele |
| POST | `/{tableId}/join` | Evet | Masaya katıl |
| POST | `/{tableId}/cancel` | Evet | Masayı iptal et |

### Wallet (`/api/v1/wallet`)
| Method | Path | Auth | Açıklama |
|--------|------|------|----------|
| GET | `/me` | Evet | Bakiye sorgula |

### Admin (`/api/v1/admin/users`) — ADMIN rolü gerekli
| Method | Path | Açıklama |
|--------|------|----------|
| GET | `/` | Kullanıcı listele (search + pagination) |
| GET | `/{id}` | Kullanıcı detay |
| POST | `/` | Kullanıcı oluştur |
| PATCH | `/{id}` | Kullanıcı güncelle |
| DELETE | `/{id}` | Kullanıcı sil |
| GET | `/{id}/wallet` | Bakiye sorgula |
| POST | `/{id}/wallet/adjust` | Bakiye ekle/çıkar |
| PUT | `/{id}/wallet` | Bakiye ayarla (mutlak) |

## WebSocket

- **Endpoint:** `ws://<host>:8080/ws` (STOMP)
- **CONNECT:** `Authorization: Bearer <access_token>` header ile
- **SUBSCRIBE:** `/topic/games/{gameId}` — sadece oyuncular subscribe olabilir
- **Heartbeat:** 10s (server↔client)

### WebSocket Event'leri (GameEvent)
```json
{ "type": "EVENT_TYPE", "gameId": 59, "tableId": 12, "bySeat": 0, "payload": { ... } }
```

| Event | payload | Açıklama |
|-------|---------|----------|
| `ROLLED` | null | Zar atıldı |
| `KEPT` | null | Zar tutuldu |
| `BANKED` | null | Puan bankalandı |
| `BUST` | null | Bust oldu |
| `TURN_CHANGED` | null | Sıra değişti |
| `FINISHED` | `{ "winnerSeat": 0 }` | Oyun bitti (hedef skora ulaşıldı) |
| `FORFEIT` | `{ "winnerSeat": 0, "loserSeat": 1, "reason": "..." }` | Forfeit (reason: VOLUNTARY / DISCONNECT / TIMEOUT / DEACTIVATED) |

## Disconnect Tespiti (Dual Mekanizma)

1. **WebSocket disconnect** → `WebSocketDisconnectListener` anında forfeit tetikler
2. **Ping-based presence** → Frontend her 10s `POST /ping` gönderir, 60s ping gelmezse `GameTimeoutJob` forfeit eder

Her iki mekanizma da `GamePlayService.forfeit()` çağırır.

## Zamanlanmış Job'lar (GameTimeoutJob)

| Job | Aralık | Timeout | Açıklama |
|-----|--------|---------|----------|
| `checkPlayerPresence` | 5s | 60s | Ping gelmeyen oyuncuyu forfeit et |
| `checkGameTimeouts` | 30s | 100dk | Absolute oyun timeout'u |
| `checkWaitingTableTimeouts` | 30s | 30dk | Bekleyen masaları iptal et + wager refund |

## Entity İlişkileri

```
User 1:1 Wallet
User 1:N WalletTxn
GameTable N:1 User (owner, seat0, seat1)
Game 1:1 GameTable
WagerLock N:1 GameTable, N:1 User (unique: table+user)
```

- `Game.stateJson` → `GameState` JSON olarak TEXT field'da saklanır
- Optimistic locking: `Game`, `GameTable`, `Wallet` entity'lerinde `@Version`

## Admin Deactivation

Kullanıcı deactivate edilince (`isActive=false`):
- Aktif oyunları otomatik forfeit edilir (reason: `DEACTIVATED`)
- JWT filter `isEnabled` kontrolü ile login engellenir

## Güvenlik

- CORS: `allowedOriginPatterns = *` (development)
- CSRF: disabled (stateless JWT)
- `anyRequest().permitAll()` — endpoint güvenliği `@PreAuthorize` ile method seviyesinde
- WebSocket auth: STOMP CONNECT'te JWT doğrulanır, SUBSCRIBE'da oyuncu kontrolü yapılır

## Kod Kuralları

- Controller → Service (interface) → Repository katmanlı mimari
- Entity → DTO dönüşümleri Mapper object'leri ile
- Eager fetch gerektiğinde `JOIN FETCH` ile JPQL query'leri kullan (`findByIdWithPlayers`, `findAllByStatusWithPlayers`)
- Lazy entity'lere transaction dışında erişme (LazyInitializationException riski)
- Circular dependency varsa `@Lazy` annotation kullan

## Build & Run

```bash
./gradlew compileKotlin          # Sadece derleme
./gradlew bootRun                # Uygulamayı başlat
./gradlew test                   # Testleri çalıştır
```

PostgreSQL (5433) ve Redis (6379) çalışıyor olmalı.
