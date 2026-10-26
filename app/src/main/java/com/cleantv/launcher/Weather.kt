package com.cleantv.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Place(val name: String, val lat: Double, val lon: Double)
data class Weather(val temp: Int, val kind: Int)

val DEFAULT_PLACE = Place("Kolkata, West Bengal, India", 22.5726, 88.3639)

// icon kinds
const val SUN = 0; const val MOON = 1; const val CLOUD = 2; const val FOG = 3
const val RAIN = 4; const val SNOW = 5; const val STORM = 6

private fun kindOf(code: Int, isDay: Boolean) = when (code) {
    0, 1 -> if (isDay) SUN else MOON
    2, 3 -> CLOUD
    45, 48 -> FOG
    in 51..67, in 80..82 -> RAIN
    in 71..77, 85, 86 -> SNOW
    in 95..99 -> STORM
    else -> CLOUD
}

private fun getJson(url: String): JSONObject {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = 8000
    return try { JSONObject(conn.inputStream.bufferedReader().use { it.readText() }) } finally { conn.disconnect() }
}

/** Current conditions from Open-Meteo (free, no account). Returns null if offline. */
suspend fun fetchWeather(p: Place, fahrenheit: Boolean): Weather? = withContext(Dispatchers.IO) {
    runCatching {
        val unit = if (fahrenheit) "&temperature_unit=fahrenheit" else ""
        val cur = getJson("https://api.open-meteo.com/v1/forecast?latitude=${p.lat}&longitude=${p.lon}" +
            "&current=temperature_2m,is_day,weather_code$unit").getJSONObject("current")
        Weather(Math.round(cur.getDouble("temperature_2m")).toInt(),
            kindOf(cur.getInt("weather_code"), cur.getInt("is_day") == 1))
    }.getOrNull()
}

suspend fun searchPlaces(query: String): List<Place> = withContext(Dispatchers.IO) {
    runCatching {
        val q = URLEncoder.encode(query, "UTF-8")
        val arr = getJson("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=6&language=en&format=json")
            .optJSONArray("results")
        val out = mutableListOf<Place>()
        if (arr != null) for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = listOf(o.optString("name"), o.optString("admin1"), o.optString("country"))
                .filter { it.isNotBlank() }.joinToString(", ")
            out += Place(name, o.getDouble("latitude"), o.getDouble("longitude"))
        }
        out.toList()
    }.getOrDefault(emptyList())
}
