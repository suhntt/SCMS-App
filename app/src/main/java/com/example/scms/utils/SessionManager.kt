package com.example.scms.utils
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*
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

    // ✅ Save logged-in user (now includes points)
    fun saveUser(user: User) {
        prefs.edit()
            .putInt("id", user.id)
            .putString("name", user.name)
            .putString("phone", user.phone)
            .putInt("points", user.points)
            .apply()
    }

    // ✅ Get logged-in user (null if not logged in)
    fun getUser(): User? {
        val id = prefs.getInt("id", -1)
        if (id == -1) return null

        return User(
            id = id,
            name = prefs.getString("name", "") ?: "",
            phone = prefs.getString("phone", "") ?: "",
            points = prefs.getInt("points", 0)
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
