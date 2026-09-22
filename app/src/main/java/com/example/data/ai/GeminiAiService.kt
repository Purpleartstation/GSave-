package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedCommand(
    val action: String, // "ADD_TRANSACTION", "GENERAL_CHAT"
    val title: String,
    val amount: Double,
    val type: String, // "Income", "Withdrawal", "Bill", "Loan"
    val category: String,
    val walletName: String,
    val reply: String
)

object GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseCommand(prompt: String, existingWallets: List<String>): ParsedCommand = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val walletsStr = existingWallets.joinToString(", ")

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackParse(prompt, existingWallets)
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", org.json.JSONArray().put(
                    JSONObject().put("parts", org.json.JSONArray().put(
                        JSONObject().put("text", "User prompt: \"$prompt\". Existing wallets: [$walletsStr]. Parse this financial command in Philippine Pesos (PHP). Return ONLY valid JSON with keys: action ('ADD_TRANSACTION' or 'GENERAL_CHAT'), title (string), amount (number), type ('Income', 'Withdrawal', 'Bill', 'Loan'), category (string), walletName (string matching one of existing wallets or default to GCash), reply (friendly confirmation string).")
                    ))
                ))
                put("systemInstruction", JSONObject().put("parts", org.json.JSONArray().put(
                    JSONObject().put("text", "You are the AI financial assistant for GSave+, an app tracking Philippine Pesos (PHP). Extract financial transactions into JSON.")
                )))
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext fallbackParse(prompt, existingWallets)
                val respBody = response.body?.string() ?: return@withContext fallbackParse(prompt, existingWallets)
                val root = JSONObject(respBody)
                val candidates = root.getJSONArray("candidates")
                val text = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                
                val cleanJson = text.replace("```json", "").replace("```", "").trim()
                val parsedObj = JSONObject(cleanJson)

                ParsedCommand(
                    action = parsedObj.optString("action", "ADD_TRANSACTION"),
                    title = parsedObj.optString("title", prompt),
                    amount = parsedObj.optDouble("amount", 0.0),
                    type = parsedObj.optString("type", "Withdrawal"),
                    category = parsedObj.optString("category", "General"),
                    walletName = parsedObj.optString("walletName", existingWallets.firstOrNull() ?: "GCash"),
                    reply = parsedObj.optString("reply", "Done! Processed your request in ₱.")
                )
            }
        } catch (e: Exception) {
            fallbackParse(prompt, existingWallets)
        }
    }

    private fun fallbackParse(prompt: String, existingWallets: List<String>): ParsedCommand {
        val lower = prompt.lowercase()
        val regex = Regex("(\\d+([kK]|\\.\\d+)?)")
        val match = regex.find(prompt)
        var amount = 500.0
        if (match != null) {
            val raw = match.value.lowercase()
            amount = if (raw.endsWith("k")) {
                (raw.removeSuffix("k").toDoubleOrNull() ?: 1.0) * 1000.0
            } else {
                raw.toDoubleOrNull() ?: 500.0
            }
        }

        val isIncome = lower.contains("add") || lower.contains("salary") || lower.contains("received") || lower.contains("income") || lower.contains("deposit")
        val type = if (isIncome) "Income" else if (lower.contains("bill") || lower.contains("meralco") || lower.contains("rent")) "Bill" else "Withdrawal"
        val category = if (lower.contains("food") || lower.contains("jollibee") || lower.contains("grocery")) "Food & Dining" else if (lower.contains("salary")) "Salary" else "General"
        
        val wallet = existingWallets.find { lower.contains(it.lowercase()) } ?: existingWallets.firstOrNull() ?: "GCash"

        return ParsedCommand(
            action = "ADD_TRANSACTION",
            title = prompt.replaceFirstChar { it.uppercase() },
            amount = amount,
            type = type,
            category = category,
            walletName = wallet,
            reply = "Successfully processed ₱%.2f for '%s' using %s!".format(amount, prompt, wallet)
        )
    }
}
