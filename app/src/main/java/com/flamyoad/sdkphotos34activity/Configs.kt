package com.flamyoad.sdkphotos34activity

import android.content.Context

class Configs(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("permissions", Context.MODE_PRIVATE)

    fun setBool(key: String, value: Boolean) {
        sharedPrefs.edit()
            .putBoolean(key, value)
            .apply()
    }

    fun getBool(key: String, valueIfNull: Boolean): Boolean {
        return sharedPrefs.getBoolean(key, valueIfNull)
    }
}