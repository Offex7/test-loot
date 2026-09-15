package com.offex7.streamhub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import okhttp3.ResponseBody
import java.io.File

private interface PlaylistApi { @GET suspend fun getPlaylist(@Url url: String): Response<ResponseBody> }
class PlaylistRepository(private val context: Context) {
 private val api = Retrofit.Builder().baseUrl("https://example.com/").build().create(PlaylistApi::class.java)
 private val cacheFile = File(context.filesDir, "tv_playlist.m3u")
 private val stampFile = File(context.filesDir, "tv_playlist.timestamp")
 suspend fun loadCachedIfFresh(): List<StreamItem>? = withContext(Dispatchers.IO) {
  if (!cacheFile.exists() || !stampFile.exists()) return@withContext null
  val age = System.currentTimeMillis() - (stampFile.readText().toLongOrNull() ?: Long.MAX_VALUE)
  if (age > 24L*60L*60L*1000L) null else parse(cacheFile.readText())
 }
 suspend fun refreshInOrder(preferredIndex: Int): Result<Pair<Int,List<StreamItem>>> = withContext(Dispatchers.IO) {
  val indexes = listOf(preferredIndex) + TV_SOURCES.indices.filter { it != preferredIndex }
  var lastError: Throwable? = null
  for (index in indexes) try {
   val response = api.getPlaylist(TV_SOURCES[index].url); val body = response.body()?.string()
   if (!response.isSuccessful || body.isNullOrBlank()) throw IllegalStateException("HTTP ${response.code()}")
   val parsed = parse(body); if (parsed.isEmpty()) throw IllegalStateException("Плейлист пуст")
   cacheFile.writeText(body); stampFile.writeText(System.currentTimeMillis().toString())
   return@withContext Result.success(index to parsed)
  } catch (t: Throwable) { lastError = t }
  Result.failure(lastError ?: IllegalStateException("Все источники недоступны"))
 }
 private fun parse(text: String): List<StreamItem> {
  val result = mutableListOf<StreamItem>(); var name: String? = null
  for (line in text.lines().map { it.trim() }.filter { it.isNotEmpty() }) {
   if (line.startsWith("#EXTINF", true)) name = line.substringAfter(',', "").trim().ifBlank { null }
   else if (!line.startsWith("#") && name != null && (line.startsWith("http://") || line.startsWith("https://"))) { result += StreamItem(name!!, line); name = null }
  }
  return result
 }
}
