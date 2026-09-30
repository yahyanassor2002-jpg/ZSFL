package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Role
import com.example.data.model.UserEntity

class SessionManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("upenjanet_session", Context.MODE_PRIVATE)

  companion object {
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"
    private const val KEY_ZONE_ID = "zone_id"
  }

  fun saveUserSession(user: UserEntity) {
    prefs.edit()
      .putBoolean(KEY_IS_LOGGED_IN, true)
      .putString(KEY_USER_ID, user.id)
      .putString(KEY_USERNAME, user.username)
      .putString(KEY_ROLE, user.role.name)
      .putString(KEY_ZONE_ID, user.zoneId)
      .apply()
  }

  fun getSavedSession(): UserEntity? {
    val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    if (!isLoggedIn) return null

    val id = prefs.getString(KEY_USER_ID, null) ?: return null
    val username = prefs.getString(KEY_USERNAME, "") ?: ""
    val roleStr = prefs.getString(KEY_ROLE, Role.HEADMAN.name) ?: Role.HEADMAN.name
    val role = runCatching { Role.valueOf(roleStr) }.getOrDefault(Role.HEADMAN)
    val zoneId = prefs.getString(KEY_ZONE_ID, null)

    return UserEntity(
      id = id,
      username = username,
      passwordHash = "",
      role = role,
      zoneId = zoneId,
    )
  }

  fun saveUserAvatar(username: String, uri: String) {
    prefs.edit().putString("avatar_$username", uri).apply()
  }

  fun getUserAvatar(username: String): String? {
    return prefs.getString("avatar_$username", null)
  }

  fun clearSession() {
    prefs.edit().clear().apply()
  }
}
