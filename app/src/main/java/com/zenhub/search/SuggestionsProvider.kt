package com.zenhub.search

import android.content.SearchRecentSuggestionsProvider

class RecentSearchesProvider : SearchRecentSuggestionsProvider() {

    init {
        setupSuggestions(Companion.AUTHORITY, Companion.MODE)
    }

    companion object {
        const val AUTHORITY = "com.zenhub.search.RecentSearchesProvider"
        const val MODE = DATABASE_MODE_QUERIES
    }
}