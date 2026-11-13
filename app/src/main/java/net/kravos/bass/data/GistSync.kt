package net.kravos.bass.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SyncError(message: String) : Exception(message)

/**
 * Stores the sync document as a secret GitHub Gist of the user's own account (file kravos-bas.json).
 * Needs a classic personal access token with only the "gist" scope. Same protocol as web/sync.js.
 */
class GistSync(private val token: String) {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    private fun req(url: String) = Request.Builder().url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("Cache-Control", "no-cache")

    private fun call(r: Request): String = client.newCall(r).execute().use { resp ->
        val body = resp.body?.string().orEmpty()
        when {
            resp.isSuccessful -> body
            resp.code == 401 -> throw SyncError("Token nije ispravan ili je istekao.")
            resp.code == 403 || resp.code == 404 -> throw SyncError("Token nema dozvolu „gist“ (HTTP ${resp.code}).")
            else -> throw SyncError("GitHub greška HTTP ${resp.code}.")
        }
    }

    /** Finds the user's sync gist; returns its id or null. */
    suspend fun find(): String? = withContext(Dispatchers.IO) {
        for (page in 1..10) {
            val arr = org.json.JSONArray(call(req("$API/gists?per_page=100&page=$page").build()))
            for (i in 0 until arr.length()) {
                val g = arr.getJSONObject(i)
                if (g.optJSONObject("files")?.has(FILE) == true) return@withContext g.getString("id")
            }
            if (arr.length() < 100) break
        }
        null
    }

    suspend fun create(doc: SyncDoc): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("description", DESCRIPTION)
            .put("public", false)
            .put("files", JSONObject().put(FILE, JSONObject().put("content", doc.toJson())))
        JSONObject(call(req("$API/gists").post(body.toString().toRequestBody(JSON)).build())).getString("id")
    }

    /** Reads the sync document; null when the gist is gone. */
    suspend fun read(id: String): SyncDoc? = withContext(Dispatchers.IO) {
        try {
            val g = JSONObject(call(req("$API/gists/$id").build()))
            val f = g.optJSONObject("files")?.optJSONObject(FILE) ?: return@withContext null
            SyncDoc.fromJson(f.optString("content"))
        } catch (e: SyncError) {
            if (e.message?.contains("404") == true) null else throw e
        }
    }

    suspend fun write(id: String, doc: SyncDoc) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("files", JSONObject().put(FILE, JSONObject().put("content", doc.toJson())))
        call(req("$API/gists/$id").patch(body.toString().toRequestBody(JSON)).build())
        Unit
    }

    companion object {
        const val API = "https://api.github.com"
        const val FILE = "kravos-bas.json"
        const val DESCRIPTION = "Kravos Bas — napredak (sinhronizacija telefon ↔ web)"
        const val TOKEN_URL = "https://github.com/settings/tokens/new?scopes=gist&description=Kravos%20Bas%20sinhronizacija"
        private val JSON = "application/json".toMediaType()
    }
}
