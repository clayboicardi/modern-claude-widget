package com.clayboicardi.claudewidget

import java.net.URLEncoder

object LaunchUri {
    /** Build a launch URI, URL-encoding an optional `q=` query (spaces as %20). */
    fun build(base: String, query: String? = null): String {
        if (query.isNullOrEmpty()) return base
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return "$base?q=$encoded"
    }
}
