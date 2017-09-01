package com.zenhub.github

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.google.gson.reflect.TypeToken
import com.zenhub.Application

val languageColors = loadLanguageColors()

class LanguageColor(val color: String/*, val url: String*/)

fun getLanguageColor(language: String?): ColorDrawable {
    return if (language == null) {
        ColorDrawable(Color.TRANSPARENT)
    } else {
        val color = languageColors[language]?.color
        if (color == null) ColorDrawable(Color.TRANSPARENT) else ColorDrawable(Color.parseColor(color))
    }
}

private fun loadLanguageColors(): Map<String, LanguageColor> {
    return Application.context.assets.open("colors.json").use {
        val typeToken = object : TypeToken<Map<String, LanguageColor>>() {}.type
        GSON.fromJson<Map<String, LanguageColor>>(it.reader(), typeToken)
    }
}
