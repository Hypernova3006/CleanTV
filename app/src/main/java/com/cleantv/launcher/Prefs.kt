package com.cleantv.launcher

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("cleantv", Context.MODE_PRIVATE)
    var hidden: Set<String>
        get() = sp.getStringSet("hidden", emptySet())!!.toSet()
        set(v) { sp.edit().putStringSet("hidden", v.toSet()).apply() }
    /** "preset:<index>" or "uri:<content uri>" */
    var wallpaper: String
        get() = sp.getString("wallpaper", "preset:0")!!
        set(v) { sp.edit().putString("wallpaper", v).apply() }
}
