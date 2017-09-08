package com.zenhub.config

import android.support.v7.app.AppCompatDelegate
import com.pddstudio.highlightjs.models.Theme

fun getHighlightJsTheme(): Theme {
    return if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO)
        Theme.GRUVBOX_LIGHT else Theme.GRUVBOX_DARK
}

private const val STYLESHEET_DARK = """
    <style>
        body {color: #ffffff; background-color: #424242;}
        a {color: #458588;}
        pre {overflow: auto; width: 99%; background-color: #424242;}
    </style>"""

private const val STYLESHEET_LIGHT = """
    <style>
        a {color: #458588;}
        pre {overflow: auto; width: 99%;}
    </style>"""
fun getReadMeStylesheet(): String {
    return if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO)
        STYLESHEET_LIGHT else STYLESHEET_DARK

}