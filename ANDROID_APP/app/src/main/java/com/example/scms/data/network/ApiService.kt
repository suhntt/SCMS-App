package com.example.scms.data.network
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ===============================
    // USER SIDE
    // ===============================

    @GET("complaints")
    suspend fun getComplaints(
        @Query("district") district: String? = null   // ✅ Filter by Assam district
    ): List<Complaint>

    @GET("complaint/{id}/history")
    suspend fun getComplaintHistory(
        @Path("id") id: String
    ): List<StatusHistoryResponse>

    @PUT("complaint/status/{id}")
    suspend fun updateComplaintStatus(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @POST("upvote/{id}")
    suspend fun upvote(
        @Path("id") id: String,
        @Body body: Map<String, Int>
    ): Response<Unit>

    @POST("complaint/verify/{id}")
    suspend fun verifyComplaint(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<Unit>

    @POST("complaint/check-duplicate")
    suspend fun checkDuplicateComplaint(
        @Body body: Map<String, String>
    ): Response<DuplicateCheckResponse>

    @Multipart
    @POST("complaint")
    suspend fun submitComplaint(
        @Part photos: List<MultipartBody.Part>,
        @Part("title") title: RequestBody,
        @Part("category") category: RequestBody,
        @Part("address") address: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("description") description: RequestBody,
        @Part("user_id") userId: RequestBody,
        @Part("district") district: RequestBody     // ✅ Detected Assam district
    ): Response<Unit>

    @POST("login")
    suspend fun login(
        @Body body: Map<String, String>
    ): Response<LoginResponse>

    @POST("signup")
    suspend fun signup(
        @Body body: Map<String, String>
    ): Response<Map<String, Boolean>>

    @PUT("user/token")
    suspend fun updateToken(
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET("district")
    suspend fun detectDistrict(
        @Query("lat") lat: String,
        @Query("lon") lon: String
    ): DistrictResponse

    @PUT("user/district")
    suspend fun updateUserDistrict(
        @Body body: Map<String, String>
    ): Response<Unit>

    @PUT("user/location")
    suspend fun updateUserLocation(
        @Body body: Map<String, String>
    ): Response<Unit>

    // ===============================
    // 🏆 GAMIFICATION
    // ===============================

    @GET("leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @GET("user/{id}/points")
    suspend fun getUserPoints(
        @Path("id") id: Int
    ): Response<PointsResponse>

    @Multipart
    @POST("user/{id}/profile")
    suspend fun updateProfile(
        @Path("id") id: String,
        @Part photo: MultipartBody.Part
    ): Response<ProfileUpdateResponse>

    // ===============================
    // ADMIN SIDE
    // ===============================

    @PUT("complaint/department/{id}")
    suspend fun assignDepartment(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @POST("complaint/resolve/{id}")
    suspend fun markResolved(
        @Path("id") id: String
    ): Response<Unit>

    // ===============================
    // 🚨 ALERTS
    // ===============================

    @GET("alerts")
    suspend fun getAlerts(
        @Query("district") district: String? = null  // ✅ Filter by Assam district
    ): List<Alert>

    // ===============================
    // 🚦 EMERGENCY SOS / ACCIDENTS
    // ===============================

    @POST("accidents")
    suspend fun postAccident(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>
}
