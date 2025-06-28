package com.example.checkbambufromandroid.di

import com.example.checkbambufromandroid.data.BambuRepository
import com.example.checkbambufromandroid.data.BambuRepositoryImpl
import com.example.checkbambufromandroid.network.BambuLabApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBambuRepository(impl: BambuRepositoryImpl): BambuRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // In-memory cookie jar for simplicity. For persistence, a custom implementation would be needed.
    class SessionCookieJar : CookieJar {
        private var cookies = mutableListOf<Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            // Filter out cookies with no name, which can happen with some servers/proxies
            // and OkHttp doesn't like them.
            val validCookies = cookies.filter { it.name.isNotBlank() }
            this.cookies.removeAll { existingCookie ->
                validCookies.any { newCookie ->
                    newCookie.name == existingCookie.name && newCookie.domain == existingCookie.domain
                }
            }
            this.cookies.addAll(validCookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            // Remove expired cookies
            cookies.removeAll { it.expiresAt < System.currentTimeMillis() }
            // Return only cookies that match the domain and path
            return cookies.filter { it.matches(url) }
        }
    }

    @Provides
    @Singleton
    fun provideCookieJar(): CookieJar {
        return SessionCookieJar()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(cookieJar: CookieJar): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Use Level.BASIC or NONE for release builds
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .cookieJar(cookieJar) // Add the cookie jar
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // Base URL for emulator to connect to localhost
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideBambuLabApiService(retrofit: Retrofit): BambuLabApiService {
        return retrofit.create(BambuLabApiService::class.java)
    }
}
