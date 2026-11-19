package com.kalogsc.feedbil.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * One HTTP client for every feed request (see `DECISIONS.md`). Timeouts are
 * short on purpose: adding or refreshing a feed is an interactive action, so
 * waiting longer than this only makes the UI feel broken.
 */
object FeedHttp {

    /** Sent for every request so feed owners can identify the app. */
    const val USER_AGENT = "Feedbil/1.0 (local RSS reader)"

    fun newClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()
}
