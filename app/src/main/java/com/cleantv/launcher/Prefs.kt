package com.cleantv.launcher

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("cleantv", Context.MODE_PRIVATE)
    var hidden: Set<String>
        get() = sp.getStringSet("hidden", emptySet())!!.toSet()
        set(v) { sp.edit().putStringSet("hidden", v.toSet()).apply() }
    /** Package names in the user's chosen app order. */
    var order: List<String>
        get() = sp.getString("order", "")!!.split(",").filter { it.isNotEmpty() }
        set(v) { sp.edit().putString("order", v.joinToString(",")).apply() }
    var sounds: Boolean
        get() = sp.getBoolean("sounds", true)
        set(v) { sp.edit().putBoolean("sounds", v).apply() }
    /** Package name of the app the Live tab opens, "system" for the TV's own Live TV, or null if not chosen yet. */
    var liveApp: String?
        get() = sp.getString("liveApp", null)
        set(v) { sp.edit().putString("liveApp", v).apply() }
    var fahrenheit: Boolean
        get() = sp.getBoolean("fahrenheit", false)
        set(v) { sp.edit().putBoolean("fahrenheit", v).apply() }
    var place: Place
        get() = Place(
            sp.getString("placeName", DEFAULT_PLACE.name)!!,
            sp.getString("placeLat", null)?.toDoubleOrNull() ?: DEFAULT_PLACE.lat,
            sp.getString("placeLon", null)?.toDoubleOrNull() ?: DEFAULT_PLACE.lon)
        set(v) { sp.edit().putString("placeName", v.name).putString("placeLat", v.lat.toString())
            .putString("placeLon", v.lon.toString()).apply() }
    /** "preset:<index>" or "uri:<content uri>" */
    var wallpaper: String
        get() = sp.getString("wallpaper", "preset:0")!!
        set(v) { sp.edit().putString("wallpaper", v).apply() }
}
