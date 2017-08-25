package com.zenhub.github

import com.google.gson.reflect.TypeToken
import com.zenhub.Application

val languageColors = loadLanguageColors()

class LanguageColor(val color: String/*, val url: String*/)

private fun loadLanguageColors(): Map<String, LanguageColor> {
    return Application.context.assets.open("colors.json").use {
        val typeToken = object : TypeToken<Map<String, LanguageColor>>() {}.type
        Application.GSON.fromJson<Map<String, LanguageColor>>(it.reader(), typeToken)
    }
}