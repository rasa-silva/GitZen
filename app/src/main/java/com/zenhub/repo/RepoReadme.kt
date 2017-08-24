package com.zenhub.repo

import android.graphics.Color
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
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

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
    webView.setBackgroundColor(Color.TRANSPARENT)
    refreshLayout.viewTreeObserver.addOnScrollChangedListener {
        refreshLayout.isEnabled = webView.scrollY == 0
    }

    refreshLayout?.setOnRefreshListener { requestReadMeData(fullRepoName, refreshLayout) }
    requestReadMeData(fullRepoName, refreshLayout)
    return view
}

private fun requestReadMeData(fullRepoName: String, rootView: SwipeRefreshLayout) {
    launch(UI) {
        Log.d(Application.LOGTAG, "Refreshing repo information...")
        val repoDetails = GitHubApi.service.repoDetails(fullRepoName).awaitResult()
        when (repoDetails) {
            is Result.Ok -> rootView.findViewById<TextView>(R.id.fullName).text = repoDetails.value.full_name
            is Result.Error -> TODO()
            is Result.Exception -> TODO()
        }

        val readMeResult = GitHubApi.service.repoReadme(fullRepoName).awaitResult()
        when (readMeResult) {
            is Result.Ok -> {
                val webView = rootView.findViewById<WebView>(R.id.readme_webview)
                val content = styleSheet + readMeResult.value.string()
                webView.loadDataWithBaseURL("https://github.com", content, "text/html", "UTF-8", null)
            }
            is Result.Error -> TODO()
            is Result.Exception -> TODO()
        }

        rootView.isRefreshing = false
    }
}