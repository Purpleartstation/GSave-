package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.entity.TransactionEntity
import com.example.data.entity.WalletEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class SupabaseSyncStatus {
    object Idle : SupabaseSyncStatus()
    object Syncing : SupabaseSyncStatus()
    data class Connected(val message: String) : SupabaseSyncStatus()
    data class Error(val error: String) : SupabaseSyncStatus()
    object NotConfigured : SupabaseSyncStatus()
}

object SupabaseService {
    private const val TAG = "SupabaseService"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var currentAuthToken: String? = null

    /**
     * Sanitizes and normalizes Supabase Project URL from BuildConfig.
     * Handles markdown formatted URLs like "[https://...](https://...)", quotes, and trailing slashes.
     */
    fun getCleanSupabaseUrl(): String {
        val raw = BuildConfig.EXPO_PUBLIC_SUPABASE_URL.trim()
        if (raw.isBlank() || raw == "https://your-project.supabase.co") {
            return ""
        }

        var url = raw
        // Handle markdown link format e.g. "[https://xyz.supabase.co](https://xyz.supabase.co)"
        if (url.contains("](") && url.endsWith(")")) {
            val start = url.indexOf("](") + 2
            url = url.substring(start, url.length - 1).trim()
        } else if (url.startsWith("[") && url.endsWith("]")) {
            url = url.removeSurrounding("[", "]").trim()
        }

        url = url.removeSurrounding("\"", "").removeSurrounding("'", "").trim()

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        return url.trimEnd('/')
    }

    fun getAnonKey(): String {
        return BuildConfig.EXPO_PUBLIC_SUPABASE_ANON_KEY.trim()
    }

    fun isConfigured(): Boolean {
        val url = getCleanSupabaseUrl()
        val key = getAnonKey()
        return url.isNotBlank() && key.isNotBlank() && key != "your-supabase-anon-key"
    }

    private fun buildRequest(url: String, method: String = "GET", body: String? = null): Request {
        val key = getAnonKey()
        val token = currentAuthToken ?: key

        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation,resolution=merge-duplicates")

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post((body ?: "{}").toRequestBody(JSON_MEDIA_TYPE))
            "PATCH" -> builder.patch((body ?: "{}").toRequestBody(JSON_MEDIA_TYPE))
            "DELETE" -> builder.delete((body ?: "{}").toRequestBody(JSON_MEDIA_TYPE))
            else -> builder.method(method, body?.toRequestBody(JSON_MEDIA_TYPE))
        }

        return builder.build()
    }

    // -------------------------------------------------------------
    // Authentication Operations
    // -------------------------------------------------------------

    suspend fun registerOrSignInUser(email: String, pin: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.success("Local mode: Supabase keys not configured")
        }

        val baseUrl = getCleanSupabaseUrl()
        // Supabase GoTrue Auth requires passwords to be at least 6 characters.
        // We append a deterministic suffix to the user's PIN.
        val password = "${pin}_GSave#${pin}"

        try {
            // 1. Try Signup
            val signUpBody = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", JSONObject().apply {
                    put("name", name)
                    put("full_name", name)
                })
            }.toString()

            val signUpReq = buildRequest(
                url = "$baseUrl/auth/v1/signup",
                method = "POST",
                body = signUpBody
            )

            client.newCall(signUpReq).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                Log.d(TAG, "Supabase Auth signup response code: ${response.code}, body: $respStr")

                if (response.isSuccessful) {
                    val json = JSONObject(respStr)
                    val token = json.optString("access_token", "")
                    if (token.isNotBlank()) {
                        currentAuthToken = token
                    }
                    upsertProfile(email, name)
                    return@withContext Result.success("Supabase registration successful")
                } else if (respStr.contains("already registered", ignoreCase = true) || response.code == 400) {
                    // User already registered, attempt sign-in
                    return@withContext signInUser(email, password, name)
                } else {
                    // Fallback to local
                    Log.w(TAG, "Supabase signup returned ${response.code}: $respStr")
                    return@withContext Result.success("Supabase auth responded: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Supabase auth: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    private suspend fun signInUser(email: String, password: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = getCleanSupabaseUrl()
        try {
            val signInBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }.toString()

            val req = buildRequest(
                url = "$baseUrl/auth/v1/token?grant_type=password",
                method = "POST",
                body = signInBody
            )

            client.newCall(req).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                Log.d(TAG, "Supabase signin response code: ${response.code}")
                if (response.isSuccessful) {
                    val json = JSONObject(respStr)
                    val token = json.optString("access_token", "")
                    if (token.isNotBlank()) {
                        currentAuthToken = token
                    }
                    upsertProfile(email, name)
                    return@withContext Result.success("Supabase sign in successful")
                } else {
                    return@withContext Result.success("Local auth authenticated (Supabase user exists)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Supabase sign in: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    private suspend fun upsertProfile(email: String, name: String) = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getCleanSupabaseUrl()
            val profileBody = JSONObject().apply {
                put("email", email)
                put("name", name)
                put("updated_at", System.currentTimeMillis())
            }.toString()

            val req = buildRequest(
                url = "$baseUrl/rest/v1/profiles",
                method = "POST",
                body = profileBody
            )
            client.newCall(req).execute().use { resp ->
                Log.d(TAG, "Supabase profile sync response code: ${resp.code}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Optional profile upsert warning: ${e.message}")
        }
    }

    // -------------------------------------------------------------
    // Wallets Database Operations
    // -------------------------------------------------------------

    suspend fun fetchWallets(): List<WalletEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val baseUrl = getCleanSupabaseUrl()

        try {
            val req = buildRequest(url = "$baseUrl/rest/v1/wallets?select=*&order=id.asc")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Fetch wallets failed with code: ${response.code}")
                    return@withContext emptyList()
                }
                val respStr = response.body?.string() ?: return@withContext emptyList()
                val jsonArr = JSONArray(respStr)
                val list = mutableListOf<WalletEntity>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    list.add(
                        WalletEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", "Wallet"),
                            type = obj.optString("type", "Cash"),
                            balance = obj.optDouble("balance", 0.0),
                            isShared = obj.optBoolean("is_shared", obj.optBoolean("isShared", false))
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wallets from Supabase: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun insertWallet(wallet: WalletEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val baseUrl = getCleanSupabaseUrl()

        try {
            val json = JSONObject().apply {
                if (wallet.id > 0L) {
                    put("id", wallet.id)
                }
                put("name", wallet.name)
                put("type", wallet.type)
                put("balance", wallet.balance)
                put("is_shared", wallet.isShared)
            }

            val req = buildRequest(
                url = "$baseUrl/rest/v1/wallets",
                method = "POST",
                body = json.toString()
            )

            client.newCall(req).execute().use { resp ->
                val isOk = resp.isSuccessful
                Log.d(TAG, "Insert wallet '${wallet.name}' to Supabase result: ${resp.code}")
                return@withContext isOk
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting wallet to Supabase: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun updateWalletBalance(walletId: Long, newBalance: Double): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val baseUrl = getCleanSupabaseUrl()

        try {
            val json = JSONObject().apply {
                put("balance", newBalance)
            }

            val req = buildRequest(
                url = "$baseUrl/rest/v1/wallets?id=eq.$walletId",
                method = "PATCH",
                body = json.toString()
            )

            client.newCall(req).execute().use { resp ->
                val isOk = resp.isSuccessful
                Log.d(TAG, "Update wallet $walletId balance to $newBalance on Supabase result: ${resp.code}")
                return@withContext isOk
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wallet balance on Supabase: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun deleteWallet(walletId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val baseUrl = getCleanSupabaseUrl()

        try {
            val req = buildRequest(
                url = "$baseUrl/rest/v1/wallets?id=eq.$walletId",
                method = "DELETE"
            )

            client.newCall(req).execute().use { resp ->
                return@withContext resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting wallet on Supabase: ${e.message}", e)
            return@withContext false
        }
    }

    // -------------------------------------------------------------
    // Transactions Database Operations
    // -------------------------------------------------------------

    suspend fun fetchTransactions(): List<TransactionEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val baseUrl = getCleanSupabaseUrl()

        try {
            val req = buildRequest(url = "$baseUrl/rest/v1/transactions?select=*&order=date.desc")
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Fetch transactions failed with code: ${response.code}")
                    return@withContext emptyList()
                }
                val respStr = response.body?.string() ?: return@withContext emptyList()
                val jsonArr = JSONArray(respStr)
                val list = mutableListOf<TransactionEntity>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    list.add(
                        TransactionEntity(
                            id = obj.optLong("id", 0L),
                            walletId = obj.optLong("wallet_id", obj.optLong("walletId", 0L)),
                            title = obj.optString("title", "Transaction"),
                            amount = obj.optDouble("amount", 0.0),
                            type = obj.optString("type", "Withdrawal"),
                            category = obj.optString("category", "General"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            recurringType = obj.optString("recurring_type", obj.optString("recurringType", "None")),
                            recurringCount = obj.optInt("recurring_count", obj.optInt("recurringCount", 0)),
                            isPaid = obj.optBoolean("is_paid", obj.optBoolean("isPaid", true))
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions from Supabase: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val baseUrl = getCleanSupabaseUrl()

        try {
            val json = JSONObject().apply {
                if (transaction.id > 0L) {
                    put("id", transaction.id)
                }
                put("wallet_id", transaction.walletId)
                put("title", transaction.title)
                put("amount", transaction.amount)
                put("type", transaction.type)
                put("category", transaction.category)
                put("date", transaction.date)
                put("recurring_type", transaction.recurringType)
                put("recurring_count", transaction.recurringCount)
                put("is_paid", transaction.isPaid)
            }

            val req = buildRequest(
                url = "$baseUrl/rest/v1/transactions",
                method = "POST",
                body = json.toString()
            )

            client.newCall(req).execute().use { resp ->
                val isOk = resp.isSuccessful
                Log.d(TAG, "Insert transaction '${transaction.title}' to Supabase result: ${resp.code}")
                return@withContext isOk
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting transaction to Supabase: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun deleteTransaction(transactionId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val baseUrl = getCleanSupabaseUrl()

        try {
            val req = buildRequest(
                url = "$baseUrl/rest/v1/transactions?id=eq.$transactionId",
                method = "DELETE"
            )

            client.newCall(req).execute().use { resp ->
                return@withContext resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting transaction from Supabase: ${e.message}", e)
            return@withContext false
        }
    }

    // -------------------------------------------------------------
    // Partner Vault & Linking Backend Operations
    // -------------------------------------------------------------

    suspend fun createPartnerVaultInCloud(vaultName: String, inviteCode: String, creatorName: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val baseUrl = getCleanSupabaseUrl()

        try {
            val json = JSONObject().apply {
                put("vault_name", vaultName)
                put("invite_code", inviteCode.uppercase().trim())
                put("creator_name", creatorName)
                put("is_connected", false)
            }

            val req = buildRequest(
                url = "$baseUrl/rest/v1/partner_vaults",
                method = "POST",
                body = json.toString()
            )

            client.newCall(req).execute().use { resp ->
                Log.d(TAG, "Create partner vault in Supabase response: ${resp.code}")
                return@withContext resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating partner vault in Supabase: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun linkPartnerVaultWithCode(inviteCode: String, partnerName: String): Result<Pair<String, Boolean>> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.success(Pair("Local link: $inviteCode", true))
        }
        val baseUrl = getCleanSupabaseUrl()
        val cleanCode = inviteCode.trim().uppercase()

        try {
            // 1. First attempt calling the RPC function 'link_partner_vault'
            val rpcBody = JSONObject().apply {
                put("p_invite_code", cleanCode)
                put("p_partner_name", partnerName)
            }.toString()

            val rpcReq = buildRequest(
                url = "$baseUrl/rest/v1/rpc/link_partner_vault",
                method = "POST",
                body = rpcBody
            )

            client.newCall(rpcReq).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: ""
                Log.d(TAG, "link_partner_vault RPC response ${resp.code}: $bodyStr")
                if (resp.isSuccessful && bodyStr.isNotBlank()) {
                    val resJson = JSONObject(bodyStr)
                    val success = resJson.optBoolean("success", true)
                    val vName = resJson.optString("vault_name", "Shared Household Vault")
                    if (success) {
                        return@withContext Result.success(Pair(vName, true))
                    }
                }
            }

            // 2. Direct PostgREST query fallback
            val queryReq = buildRequest(
                url = "$baseUrl/rest/v1/partner_vaults?invite_code=eq.$cleanCode&select=*",
                method = "GET"
            )

            client.newCall(queryReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.success(Pair("Shared Household Vault ($cleanCode)", true))
                }
                val respStr = resp.body?.string() ?: "[]"
                val arr = JSONArray(respStr)
                if (arr.length() > 0) {
                    val vaultObj = arr.getJSONObject(0)
                    val vaultName = vaultObj.optString("vault_name", "Shared Household Vault")

                    // Mark as connected in Supabase
                    val patchBody = JSONObject().apply {
                        put("is_connected", true)
                        put("partner_name", partnerName)
                    }.toString()

                    val patchReq = buildRequest(
                        url = "$baseUrl/rest/v1/partner_vaults?invite_code=eq.$cleanCode",
                        method = "PATCH",
                        body = patchBody
                    )
                    client.newCall(patchReq).execute().close()

                    return@withContext Result.success(Pair(vaultName, true))
                } else {
                    return@withContext Result.success(Pair("Shared Household Vault ($cleanCode)", true))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error linking partner vault: ${e.message}", e)
            return@withContext Result.success(Pair("Shared Household Vault ($cleanCode)", true))
        }
    }

    suspend fun checkPartnerConnection(inviteCode: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured() || inviteCode.isBlank()) return@withContext false
        val baseUrl = getCleanSupabaseUrl()
        val cleanCode = inviteCode.trim().uppercase()

        try {
            val req = buildRequest(
                url = "$baseUrl/rest/v1/partner_vaults?invite_code=eq.$cleanCode&select=is_connected,partner_name",
                method = "GET"
            )
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                val respStr = resp.body?.string() ?: "[]"
                val arr = JSONArray(respStr)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    return@withContext obj.optBoolean("is_connected", false)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Check partner connection warning: ${e.message}")
        }
        return@withContext false
    }
}

