package com.cpen321.usermanagement.data.remote.api

import android.util.Log
import com.cpen321.usermanagement.BuildConfig
import com.cpen321.usermanagement.data.remote.interceptors.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL
    private const val IMAGE_BASE_URL = BuildConfig.IMAGE_BASE_URL

    private const val NHL_BASE_URL = BuildConfig.NHL_BASE_URL


    private var authToken: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Enhanced logging for NHL API debugging
    private val nhlLoggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("NHL_API", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = AuthInterceptor { authToken }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Separate HTTP client for NHL API (no auth needed)
    private val nhlHttpClient = OkHttpClient.Builder()
        .addInterceptor(nhlLoggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val nhlRetrofit = Retrofit.Builder()
        .baseUrl(NHL_BASE_URL)
        .client(nhlHttpClient) // Use separate client without auth
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val nhlInterface: NHLInterface = nhlRetrofit.create(NHLInterface::class.java)
    val authInterface: AuthInterface = retrofit.create(AuthInterface::class.java)
    val imageInterface: ImageInterface = retrofit.create(ImageInterface::class.java)
    val userInterface: UserInterface = retrofit.create(UserInterface::class.java)
    val friendsInterface: FriendsInterface = retrofit.create(FriendsInterface::class.java)
    val challengesInterface: ChallengesInterface = retrofit.create(ChallengesInterface::class.java)
    val ticketsInterface: TicketsInterface = retrofit.create(TicketsInterface::class.java)

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    fun getPictureUri(picturePath: String): String {
        return if (picturePath.startsWith("uploads/")) {
            // Remove trailing slash from IMAGE_BASE_URL if present, then add path
            val baseUrl = IMAGE_BASE_URL.trimEnd('/')
            "$baseUrl/$picturePath"
        } else {
            picturePath
        }
    }
}