package com.example.scms.utils
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("scms_prefs", Context.MODE_PRIVATE)

    // ✅ Save logged-in user (ALL fields persisted)
    fun saveUser(user: User) {
        prefs.edit()
            .putInt("id", user.id)
            .putString("name", user.name)
            .putString("phone", user.phone)
            .putString("email", user.email ?: "")
            .putInt("points", user.points)
            .putString("profile_picture", user.profile_picture)
            .putString("district", user.district)
            .putString("badgeLevel", user.badgeLevel)
            .putString("firebaseUid", user.firebaseUid)
            .apply()
    }

    // ✅ Get logged-in user — restores complete profile
    fun getUser(): User? {
        val id = prefs.getInt("id", -1)
        if (id == -1) return null

        return User(
            id = id,
            firebaseUid = prefs.getString("firebaseUid", null),
            name = prefs.getString("name", "") ?: "",
            phone = prefs.getString("phone", "") ?: "",
            email = prefs.getString("email", null),
            points = prefs.getInt("points", 0),
            profile_picture = prefs.getString("profile_picture", null),
            district = prefs.getString("district", null),
            badgeLevel = prefs.getString("badgeLevel", "Citizen") ?: "Citizen"
        )
    }

    // ✅ Update points without re-login
    fun updatePoints(points: Int) {
        prefs.edit().putInt("points", points).apply()
    }

    // ✅ Clear session on logout
    fun clear() {
        prefs.edit().clear().apply()
    }
}
