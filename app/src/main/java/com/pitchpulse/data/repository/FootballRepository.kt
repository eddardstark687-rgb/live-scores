package com.pitchpulse.data.repository

import android.util.Log
import com.pitchpulse.data.local.dao.FavoriteTeamDao
import com.pitchpulse.data.local.dao.FootballDao
import com.pitchpulse.data.local.entity.FavoriteTeamEntity
import com.pitchpulse.data.local.entity.FetchMetadataEntity
import com.pitchpulse.data.local.entity.MatchEntity
import com.pitchpulse.data.model.Lineup
import com.pitchpulse.data.model.Match
import com.pitchpulse.data.model.MatchStatistics
import com.pitchpulse.data.model.StatItem
import com.pitchpulse.data.remote.FootballApi
import com.pitchpulse.data.remote.dto.FixtureWrapperDto
import com.pitchpulse.data.remote.dto.LineupDto
import com.pitchpulse.data.remote.dto.StatisticDto
import com.pitchpulse.data.local.entity.LineupEntity
import com.pitchpulse.data.local.entity.StatisticEntity
import com.pitchpulse.data.local.entity.ApiUsageEntity
import com.pitchpulse.data.remote.dto.TeamDto
import com.pitchpulse.data.remote.dto.toDomain
import com.pitchpulse.data.remote.dto.toDomainItems
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private const val TAG              = "API_DEBUG"
private const val STALE_MILLIS     = 60 * 60 * 1000L  // 1 hour – non-live data
private const val LIVE_REFRESH_MILLIS = 2 * 60 * 1000L   // 2 minutes – live throttle
private const val COOLDOWN_MILLIS  = 5 * 60 * 1000L   // 5 minutes – retry after failure
private const val FAVORITE_SYNC_THROTTLE = 15 * 60 * 1000L // 15 minutes

class ApiException(message: String) : Exception(message)

class FootballRepository(
    private val api: FootballApi,
    private val dao: FootballDao,
    private val favoriteTeamDao: FavoriteTeamDao
) {

    // In-memory guard: date → timestamp of last successful fetch this process lifetime.
    private val inMemoryFetchTimes = ConcurrentHashMap<String, Long>()
    
    // Throttling for detail fetches: fixtureId/teamId → timestamp
    private val lastDetailFetchTimes = ConcurrentHashMap<String, Long>()

    // Per-date mutex to prevent concurrent API calls for the same date.
    private val dateMutexes = ConcurrentHashMap<String, Mutex>()

    private var lastFavoriteSyncTime = 0L

    companion object {
        private val WHITELISTED_CLUB_LEAGUES = setOf(
            39, 45, 48, 46,             // England: Premier League, FA Cup, League Cup, Community Shield
            140, 143, 141,              // Spain: LaLiga, Copa del Rey, Supercopa
            135, 137, 549,              // Italy: Serie A, Coppa Italia, Supercoppa
            61, 66, 65,                 // France: Ligue 1, Coupe de France, Trophée des Champions
            78, 81, 529,                // Germany: Bundesliga, DFB Pokal, DFL-Supercup
            2, 3, 848                   // UEFA: Champions League, Europa League, Conference League
        )
        private val WHITELISTED_NATIONAL_TOURS = setOf(
            1,                          // World Cup
            4, 5, 33,                   // UEFA: Euro, Nations League, Qualifiers
            13, 22, 31,                 // CONCACAF: Nations League, Gold Cup, Qualifiers
            9, 30,                      // CONMEBOL: Copa America, Qualifiers
            10                          // International Friendlies
        )
    }

    internal fun isLeagueAllowed(leagueId: Int) =
        leagueId in WHITELISTED_CLUB_LEAGUES || leagueId in WHITELISTED_NATIONAL_TOURS

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Pure DB Flow for [date]. NO network side-effects. UI subscribes here.
     */
    fun getDailyMatches(date: String): Flow<List<Match>> =
        dao.getDailyMatchesFlow(date).map { it.map { e -> e.toDomainModel() } }

    /**
     * Checks all throttle conditions and calls the API only when genuinely necessary.
     * Guaranteed to stay under 100 calls/day by freezing past dates and 2min stale check.
     */
    suspend fun syncIfNeeded(date: String) {
        val mutex = dateMutexes.getOrPut(date) { Mutex() }
        if (!mutex.tryLock()) {
            Log.d(TAG, "[$date] SKIPPED API – mutex locked (already syncing)")
            return
        }
        try {
            val now      = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayUtc = dateFormat.format(Date())
            val meta     = withContext(Dispatchers.IO) { dao.getMetadata(date) }

            // ── Guard 1: Freeze past dates once data exists ───────────────
            // Update: We now allow re-fetching if the data is "incomplete" (missing goal scorers)
            if (date < todayUtc) {
                if ((meta?.lastFetchTime ?: 0L) > 0L) {
                    val currentMatches = withContext(Dispatchers.IO) { dao.getDailyMatches(date) }
                    val needsBackfill = currentMatches.any { 
                        (it.homeScore ?: 0 > 0 || it.awayScore ?: 0 > 0) && it.events.isEmpty() 
                    }
                    
                    if (!needsBackfill) {
                        Log.d(TAG, "[$date] STRICT CACHE HIT – past date frozen")
                        return
                    }
                    Log.d(TAG, "[$date] RE-FETCHing for backfill (missing goal scorers)")
                } else {
                    Log.d(TAG, "[$date] FIRST FETCH for past date")
                }
                performFetch(date, now, meta)
                return
            }

            // ── Guard 2: Resolve effective last-fetch time ────────────────
            val inMemoryTime = inMemoryFetchTimes[date] ?: 0L
            val lastFetchTime = maxOf(inMemoryTime, meta?.lastFetchTime ?: 0L)
            val isLive = meta?.hasLiveMatches == true
            val ageMs  = now - lastFetchTime

            // ── Guard 3: Live throttle (60 s) ─────────────────────────────
            if (isLive) {
                val sinceLastLive = now - (meta?.lastLiveFetchTime ?: 0L)
                if (sinceLastLive < LIVE_REFRESH_MILLIS) {
                    Log.d(TAG, "[$date] SKIPPED API – live throttle (${sinceLastLive / 1000}s / ${LIVE_REFRESH_MILLIS / 1000}s)")
                    return
                }
                Log.d(TAG, "[$date] hasLiveMatches: true → refresh now")
                performFetch(date, now, meta)
                return
            }

            // ── Guard 4: Failure cooldown ─────────────────────────────────
            val lastAttempt = meta?.lastFetchAttemptTime ?: 0L
            if (lastFetchTime == 0L && lastAttempt > 0L && now - lastAttempt < COOLDOWN_MILLIS) {
                Log.d(TAG, "[$date] SKIPPED API – cooldown after failure (${(now - lastAttempt) / 1000}s)")
                return
            }

            // ── Guard 5: Cache is fresh ───────────────────────────────────
            if (lastFetchTime > 0L && ageMs < STALE_MILLIS) {
                Log.d(TAG, "[$date] CACHE USED – data is ${ageMs / 1000}s old (threshold ${STALE_MILLIS / 1000}s)")
                return
            }

            Log.d(TAG, "[$date] API CALL for date: $date (age: ${ageMs / 1000}s)")
            performFetch(date, now, meta)

        } finally {
            mutex.unlock()
        }
    }

    /** Calls the network, writes to DB only on success. NEVER clears DB on failure. */
    private suspend fun performFetch(date: String, now: Long, existingMeta: FetchMetadataEntity?) {
        if (!checkQuota()) return
        
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "[$date] API CALL TRIGGERED")
                val response = api.getFixtures(date = date)
                trackCall()

                // ── Check for Errors explicitly ───────────────────────────────
                val errStr = response.errors?.toString()
                if (errStr != null && errStr != "[]") {
                    Log.e(TAG, "[$date] API REPORTED ERROR: $errStr")
                    throw ApiException(errStr)
                }

                Log.d("API_DEBUG", "Raw response size for $date: ${response.response.size}")

                if (response.response.isNotEmpty()) {
                    val entities = response.response
                        .map { it.toEntity(date, isLeagueAllowed(it.league.id)) }

                    val hasLive = entities.any { it.isLive }

                    dao.clearDailyMatches(date)
                    dao.insertMatches(entities)
                    dao.insertMetadata(
                        FetchMetadataEntity(
                            date = date,
                            lastFetchTime = now,
                            lastFetchAttemptTime = now,
                            lastLiveFetchTime = if (hasLive) now else existingMeta?.lastLiveFetchTime ?: 0L,
                            hasLiveMatches = hasLive
                        )
                    )
                    inMemoryFetchTimes[date] = now
                    Log.d(TAG, "[$date] API SUCCESS – ${entities.size} matches")
                } else {
                    dao.insertMetadata(
                        (existingMeta ?: FetchMetadataEntity(date, 0L, now, 0L, false))
                            .copy(lastFetchAttemptTime = now)
                    )
                    Log.d(TAG, "[$date] API EMPTY – recorded attempt")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[$date] API ERROR – ${e.message}")
                dao.insertMetadata(
                    (existingMeta ?: FetchMetadataEntity(date, 0L, now, 0L, false))
                        .copy(lastFetchAttemptTime = now)
                )
            }
        }
    }

    // ── Team Details & Personalized Fixtures ─────────────────────────────────

    suspend fun getTeamInfo(teamId: Int): com.pitchpulse.data.model.TeamDetails? = withContext(Dispatchers.IO) {
        try {
            // 1. Check Cache first
            val cached = dao.getTeamDetails(teamId)
            if (cached != null) {
                Log.d(TAG, "Cache HIT for team info $teamId")
                return@withContext cached.toDomainModel()
            }

            // 2. Fetch from API
            if (!checkQuota()) return@withContext null
            
            Log.d(TAG, "Cache MISS for team info $teamId. Fetching from API.")
            val rawResponse = api.getTeamInfo(teamId)
            trackCall()
            
            val errStr = rawResponse.errors?.toString()
            if (errStr != null && errStr != "[]") {
                Log.e(TAG, "API returned errors for team info $teamId: $errStr")
            }

            val response = rawResponse.response.firstOrNull() ?: run {
                Log.w(TAG, "No team info found in API response for $teamId.")
                return@withContext null
            }

            val domainDetails = com.pitchpulse.data.model.TeamDetails(
                id = response.team.id,
                name = response.team.name,
                logo = response.team.logo,
                country = response.team.country,
                founded = response.team.founded,
                isNational = response.team.national,
                venueName = response.venue?.name,
                venueCity = response.venue?.city,
                venueCapacity = response.venue?.capacity,
                venueImage = response.venue?.image
            )

            // 3. Save to Cache
            dao.insertTeamDetails(com.pitchpulse.data.local.entity.TeamDetailsEntity.fromDomain(domainDetails))
            
            domainDetails
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in getTeamInfo for $teamId: ${e.message}")
            null
        }
    }

    suspend fun getTeamFixtures(teamId: Int, next: Int? = null, last: Int? = null): List<Match> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching fixtures for team $teamId (next=$next, last=$last)")
            
            // 1. Check local cache first
            val cachedMatches = dao.getTeamFixtures(teamId).map { it.toDomainModel() }
            val nowMs = System.currentTimeMillis()
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

            if (next != null) {
                val upcoming = cachedMatches.filter { 
                    try {
                        // Using a more reliable date parsing if needed, but dateString >= today is usually enough
                        // For simplicity, if we have any upcoming matches, we use them
                        it.time.contains(":") || it.time == "NS" // Placeholder for "not started"
                    } catch (e: Exception) { false }
                }.sortedBy { it.id } // Assume higher ID is later or use proper date parsing
                
                if (upcoming.size >= next) {
                    Log.d(TAG, "STRICT CACHE HIT for team $teamId upcoming fixtures")
                    return@withContext upcoming.take(next)
                }
            } else if (last != null) {
                val past = cachedMatches.filter { it.time == "FT" }
                if (past.size >= last) {
                    Log.d(TAG, "STRICT CACHE HIT for team $teamId past fixtures")
                    return@withContext past.take(last)
                }
            }

            // 2. Initial attempt with specific next/last filters
            if (!checkQuota()) return@withContext emptyList()
            val rawResponse = api.getTeamFixtures(teamId, next, last)
            trackCall()
            var response = rawResponse.response
            
            val errStr = rawResponse.errors?.toString()
            if (errStr != null && errStr != "[]") {
                Log.e(TAG, "API Errors for team $teamId: $errStr")
            }

            // 2. Dynamic Season Fallback - if next/last was empty or blocked, try the full current season
            if (response.isEmpty()) {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val seasonYear = if (currentMonth < 7) currentYear - 1 else currentYear
                
                Log.d(TAG, "Attempting season fallback for team $teamId with season=$seasonYear")
                if (!checkQuota()) return@withContext emptyList()
                val seasonResponse = api.getTeamFixtures(teamId, season = seasonYear)
                trackCall()
                response = seasonResponse.response
                
                val now = System.currentTimeMillis()
                val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                
                if (next != null) {
                    response = response.filter { 
                        try {
                            val fixtureDate = apiFormat.parse(it.fixture.date)?.time ?: 0L
                            fixtureDate > now
                        } catch (e: Exception) { false }
                    }.sortedBy { it.fixture.date }.take(next)
                } else if (last != null) {
                    response = response.filter { 
                        try {
                            val fixtureDate = apiFormat.parse(it.fixture.date)?.time ?: 0L
                            fixtureDate < now
                        } catch (e: Exception) { false }
                    }.sortedByDescending { it.fixture.date }.take(last)
                }
            } else {
                if (next != null) {
                    response = response.sortedBy { it.fixture.date }
                } else if (last != null) {
                    response = response.sortedByDescending { it.fixture.date }
                }
            }

            if (response.isEmpty()) {
                Log.w(TAG, "No real fixtures found for team $teamId after all attempts.")
                return@withContext emptyList()
            }

            val filteredResponse = response.filter { isLeagueAllowed(it.league.id) }

            val entities = filteredResponse.mapNotNull { dto ->
                try {
                    val dateString = dto.fixture.date.substringBefore("T")
                    dto.toEntity(dateString, true)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to map fixture ${dto.fixture.id}: ${e.message}")
                    null
                }
            }
            
            if (entities.isNotEmpty()) {
                dao.insertMatches(entities)
            }
            
            entities.map { it.toDomainModel() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in getTeamFixtures: ${e.message}")
            emptyList()
        }
    }

    suspend fun getFavoriteTeamsNextFixtures(): List<Match> = coroutineScope {
        try {
            val favorites = favoriteTeamDao.getFavoriteTeams()
            if (favorites.isEmpty()) return@coroutineScope emptyList()

            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = dateFormat.format(Date())

            // 1. First, try to get as many as possible from the Local DB
            val matchesFromDb = favorites.mapNotNull { team ->
                dao.getNextMatchForTeam(team.teamId, today)?.toDomainModel()
            }

            // If we have data for all favorites or it's been synced recently, just return what we have
            val needsSync = favorites.size > matchesFromDb.size || (now - lastFavoriteSyncTime > FAVORITE_SYNC_THROTTLE)
            
            if (!needsSync) {
                Log.d(TAG, "Favorite upcoming fixtures loaded from DB (throttled)")
                return@coroutineScope matchesFromDb.distinctBy { it.id }.sortedBy { it.id }
            }

            // 2. Only fetch missing ones from API, and only if not recently synced
            Log.d(TAG, "Syncing favorite fixtures from API (stale or missing data)")
            val teamsToSync = if (now - lastFavoriteSyncTime > FAVORITE_SYNC_THROTTLE) {
                favorites.take(5) // Limit to 5 teams per sync to save quota
            } else {
                favorites.filter { fav -> matchesFromDb.none { it.homeTeamId == fav.teamId || it.awayTeamId == fav.teamId } }.take(3)
            }

            if (teamsToSync.isEmpty()) return@coroutineScope matchesFromDb

            val apiMatches = teamsToSync.map { team ->
                async {
                    getTeamFixtures(team.teamId, next = 1).firstOrNull()
                }
            }.awaitAll().filterNotNull()

            lastFavoriteSyncTime = now
            
            (matchesFromDb + apiMatches).distinctBy { it.id }.sortedBy { it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching favorites next fixtures: ${e.message}")
            emptyList()
        }
    }

    // ── Favorites & Search & Detail ──────────────────────────────────────────

    fun getFavoriteMatches(): Flow<List<Match>> =
        dao.getFavoriteMatchesFlow().map { it.map { e -> e.toDomainModel() } }

    fun getFavoriteTeams(): Flow<List<FavoriteTeamEntity>> =
        favoriteTeamDao.getFavoriteTeamsFlow()

    suspend fun toggleFavoriteTeam(teamId: Int, name: String, logoUrl: String?) {
        if (favoriteTeamDao.isTeamFavorite(teamId)) {
            favoriteTeamDao.deleteFavoriteTeam(FavoriteTeamEntity(teamId, name, logoUrl))
        } else {
            favoriteTeamDao.insertFavoriteTeam(FavoriteTeamEntity(teamId, name, logoUrl))
        }
    }

    fun getMatchDetails(fixtureId: Int): Flow<Match?> =
        dao.getMatchByIdFlow(fixtureId).map { it?.toDomainModel() }

    suspend fun syncFixtureDetails(fixtureId: Int) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastSync = lastDetailFetchTimes["fixture_$fixtureId"] ?: 0L
        if (now - lastSync < 30_000) {
            Log.d(TAG, "syncFixtureDetails throttled for $fixtureId")
            return@withContext
        }

        if (!checkQuota()) return@withContext
        
        try {
            // Check if match is already finished and has events
            val currentMatch = dao.getMatchById(fixtureId)
            if (currentMatch != null && currentMatch.time == "FT" && currentMatch.events.isNotEmpty()) {
                Log.d(TAG, "STRICT CACHE HIT for fixture $fixtureId details")
                return@withContext
            }

            Log.d(TAG, "Syncing details for fixture $fixtureId")
            val response = api.getFixtureDetails(fixtureId)
            trackCall()
            lastDetailFetchTimes["fixture_$fixtureId"] = now
            
            val errStr = response.errors?.toString()
            if (errStr != null && errStr != "[]") {
                Log.e(TAG, "SyncFixtureDetails API ERROR: $errStr")
                return@withContext
            }

            val dto = response.response.firstOrNull() ?: return@withContext
            
            val dateString = dto.fixture.date.substringBefore("T")
            val entity = dto.toEntity(dateString, isLeagueAllowed(dto.league.id))
            dao.insertMatches(listOf(entity))
            Log.d(TAG, "SyncFixtureDetails SUCCESS for $fixtureId (events: ${entity.events.size})")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SyncFixtureDetails failed: ${e.message}")
        }
    }

    suspend fun getLineups(fixtureId: Int): List<Lineup> = withContext(Dispatchers.IO) {
        try {
            val cached = dao.getLineups(fixtureId)
            val match = dao.getMatchById(fixtureId)
            
            if (cached != null && match?.isLive == false) {
                return@withContext cached.lineups
            }

            if (!checkQuota()) return@withContext emptyList()
            val response = api.getLineups(fixtureId).response.map { it.toDomain() }
            trackCall()
            
            if (response.isNotEmpty()) {
                dao.insertLineups(LineupEntity(fixtureId, response))
            }
            response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getLineups error for $fixtureId: ${e.message}")
            emptyList()
        }
    }

    suspend fun getStatistics(fixtureId: Int): MatchStatistics = withContext(Dispatchers.IO) {
        try {
            val cached = dao.getStatistics(fixtureId)
            val match = dao.getMatchById(fixtureId)

            if (cached != null && match?.isLive == false) {
                return@withContext cached.statistics
            }

            if (!checkQuota()) return@withContext MatchStatistics(emptyList(), emptyList())
            val dtos = api.getStatistics(fixtureId).response
            trackCall()
            val stats = if (dtos.size >= 2) {
                MatchStatistics(dtos[0].toDomainItems(), dtos[1].toDomainItems())
            } else {
                MatchStatistics(emptyList(), emptyList())
            }
            
            if (stats.homeStats.isNotEmpty()) {
                dao.insertStatistics(StatisticEntity(fixtureId, stats))
            }
            stats
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "getStatistics error for $fixtureId: ${e.message}")
            MatchStatistics(emptyList(), emptyList())
        }
    }

    suspend fun searchTeams(query: String): List<TeamDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching teams with query: $query")
            if (!checkQuota()) return@withContext emptyList()
            val response = api.searchTeams(query)
            trackCall()
            
            val errStr = response.errors?.toString()
            if (errStr != null && errStr != "[]") {
                Log.e(TAG, "API SEARCH ERROR: $errStr")
            }
            response.response.map { it.team }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fatal Search Exception: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun nukeAllData() = withContext(Dispatchers.IO) { 
        dao.nukeMatches() 
        dao.clearMetadata()
    }

    private fun FixtureWrapperDto.toEntity(reqDate: String, isFavorite: Boolean): MatchEntity {
        val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US)
        timeFormatter.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        
        val date = apiFormat.parse(fixture.date) ?: Date()
        
        val istDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        istDateFormatter.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val localDateString = istDateFormatter.format(date)

        val displayTime = when (fixture.status.short) {
            "NS", "TBD" -> timeFormatter.format(date)
            "FT", "AET", "PEN" -> "FT"
            else -> fixture.status.elapsed?.let { "${it}'" } ?: fixture.status.short
        }

        val matchEvents = events.mapNotNull { eventDto ->
            val type = when (eventDto.type.lowercase(Locale.US)) {
                "goal" -> com.pitchpulse.data.model.EventType.GOAL
                "card" -> {
                    if (eventDto.detail?.lowercase(Locale.US)?.contains("red") == true) com.pitchpulse.data.model.EventType.RED_CARD
                    else com.pitchpulse.data.model.EventType.YELLOW_CARD
                }
                else -> return@mapNotNull null
            }
            com.pitchpulse.data.model.MatchEvent(
                player = eventDto.player.name ?: "Unknown",
                minute = eventDto.time.elapsed + (eventDto.time.extra ?: 0),
                type = type,
                teamId = eventDto.team.id
            )
        }

        return MatchEntity(
            id = fixture.id,
            homeTeam = teams.home.name,
            homeTeamId = teams.home.id,
            homeTeamLogo = teams.home.logo,
            awayTeam = teams.away.name,
            awayTeamId = teams.away.id,
            awayTeamLogo = teams.away.logo,
            homeScore = when (fixture.status.short) {
                "NS", "TBD", "PST", "CANC" -> goals.home
                else -> goals.home ?: 0
            },
            awayScore = when (fixture.status.short) {
                "NS", "TBD", "PST", "CANC" -> goals.away
                else -> goals.away ?: 0
            },
            time = displayTime,
            isLive = fixture.status.short in listOf("1H", "HT", "2H", "ET", "P", "BT"),
            competition = league.name,
            isFavoriteLeague = isFavorite,
            dateString = localDateString,
            events = matchEvents
        )
    }

    private suspend fun checkQuota(): Boolean = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        val usage = dao.getApiUsage(today)
        
        if (usage == null) {
            dao.insertApiUsage(com.pitchpulse.data.local.entity.ApiUsageEntity(today, 0))
            return@withContext true
        }
        
        // Multi-key support: 95 calls per key
        val currentKeyLimit = (com.pitchpulse.core.network.RetrofitClient.getActiveKeyIndex() + 1) * 95
        
        if (usage.callCount >= currentKeyLimit) {
            Log.w(TAG, "API QUOTA REACHED for current key. Attempting rotation...")
            com.pitchpulse.core.network.RetrofitClient.rotateKey()
            
            // Re-check after rotation
            val newLimit = (com.pitchpulse.core.network.RetrofitClient.getActiveKeyIndex() + 1) * 95
            if (usage.callCount >= newLimit) {
                Log.e(TAG, "ALL API KEYS EXHAUSTED for today.")
                return@withContext false
            }
        }
        true
    }

    private suspend fun trackCall() = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        dao.incrementApiUsage(today)
    }
}
