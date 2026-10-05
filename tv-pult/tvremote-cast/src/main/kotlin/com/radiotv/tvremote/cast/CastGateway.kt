package com.radiotv.tvremote.cast

data class CastTarget(val id: String, val name: String, val address: String? = null)

interface CastGateway {
    suspend fun discover(): List<CastTarget>
    suspend fun play(target: CastTarget, url: String): Result<Unit>
}
