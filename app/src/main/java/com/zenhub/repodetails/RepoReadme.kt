package com.zenhub.repodetails

import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.github.GitHubApi

private val styleSheet = """
    <style>
        body {color: #ffffff; background-color: #303030;}
        a {color: #458588;}
        pre {overflow: auto; width: 99%; background-color: #424242;}
    </style>"""

fun buildReadmeView(inflater: LayoutInflater, container: ViewGroup, fullRepoName: String): View {
    val view = inflater.inflate(R.layout.repo_content_readme, container, false)
    //Fix the fight between the refreshLayout swipe and the webview scroll
    //TODO NestedScrollView?
    val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.readme_swiperefresh)
    val webView = view.findViewById<WebView>(R.id.readme_webview)
    refreshLayout.viewTreeObserver.addOnScrollChangedListener {
        refreshLayout.isEnabled = webView.scrollY == 0
    }

    refreshLayout?.setOnRefreshListener { requestReadMeData(fullRepoName, container) }
    requestReadMeData(fullRepoName, container)
    return view
}

private fun requestReadMeData(fullRepoName: String, container: ViewGroup) {
    Log.d(Application.LOGTAG, "Refreshing repo information...")
    GitHubApi.repoDetails(fullRepoName, container) { response, rootView ->
        if (response == null) {
            Log.d(Application.LOGTAG, "Response is null. Will not update contents.")
        } else {
            rootView.findViewById<TextView>(R.id.fullName).text = response.full_name
        }
    }

    GitHubApi.readMeData(fullRepoName, container) { response, rootView ->
        if (response == null) {
            Log.d(Application.LOGTAG, "Response is null. Will not update contents.")
        } else {
            val webView = rootView.findViewById<WebView>(R.id.readme_webview)
            val content = styleSheet + response.string()
            webView.loadDataWithBaseURL("https://github.com", content, "text/html", "UTF-8", null)
        }

        rootView.findViewById<SwipeRefreshLayout>(R.id.readme_swiperefresh).isRefreshing = false
    }
}