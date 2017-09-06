package com.zenhub.core

import android.util.Log
import com.zenhub.Application
import com.zenhub.auth.LoggedUser
import okhttp3.Interceptor
import okhttp3.Response

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()

        val t1 = System.nanoTime()
        Log.d(Application.LOGTAG, String.format("Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()))

        val response = chain.proceed(request)

        val t2 = System.nanoTime()
        Log.d(Application.LOGTAG, String.format("Received response for %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6, response.headers()))

        return response
    }
}

class OAuthTokenInterceptor : Interceptor {

    private val AUTH_HEADER = "Authorization"
    private val token: String? by lazy { LoggedUser.getToken() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val oldReq = chain.request()
        val newReq = if (oldReq.header(AUTH_HEADER).isNullOrBlank() && token != null) {
            oldReq.newBuilder().addHeader(AUTH_HEADER, "token $token").build()
        } else {
            oldReq
        }

        return chain.proceed(newReq)
    }
}