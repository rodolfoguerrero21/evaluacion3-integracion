package com.example.asistenciamovil.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { newCookie ->
            this.cookies.removeAll {
                it.name == newCookie.name && it.domain == newCookie.domain && it.path == newCookie.path
            }
            if (newCookie.expiresAt > System.currentTimeMillis()) {
                this.cookies.add(newCookie)
            }
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        cookies.removeAll { it.expiresAt <= System.currentTimeMillis() }
        return cookies.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        cookies.clear()
    }
}
