package com.zenhub.repo

import android.graphics.Color
import android.support.v4.widget.SwipeRefreshLayout
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.getLanguageColor
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult


private const val STYLESHEET = """
    <style>
        body {color: #ffffff; background-color: #303030;}
        a {color: #458588;}
        pre {overflow: auto; width: 99%; background-color: #424242;}
    </style>"""

private const val EMPTY_README = """<body><h1>No ReadMe available.</h1></body>"""

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
        val repoDetails = gitHubService.repoDetails(fullRepoName).awaitResult()
        when (repoDetails) {
            is Result.Ok -> {
                val details = repoDetails.value
                rootView.findViewById<TextView>(R.id.description).text = details.description
                val homepage = if (details.homepage.isNullOrBlank()) details.html_url else details.homepage
                rootView.findViewById<TextView>(R.id.homepage).text = homepage

                rootView.findViewById<TextView>(R.id.language)?.apply {
                    text = details.language
                    background = getLanguageColor(details.language)
                }

                rootView.findViewById<TextView>(R.id.stargazers)?.apply {
                    val string = rootView.resources.getString(R.string.stargazers_count, details.stargazers_count)
                    text = Html.fromHtml(string)
                }

                rootView.findViewById<TextView>(R.id.size)?.apply {
                    val string = rootView.resources.getString(R.string.repo_size, details.size)
                    text = Html.fromHtml(string)
                }

                rootView.findViewById<TextView>(R.id.pushed_time)?.apply {
                    val string = rootView.resources.getString(R.string.last_pushed, details.pushed_at.asFuzzyDate())
                    text = Html.fromHtml(string)
                }

                //TODO Check if repo is starred and change star imageview to filled
            }
            is Result.Error -> TODO()
            is Result.Exception -> TODO()
        }

        val readMeResult = gitHubService.repoReadme(fullRepoName).awaitResult()
        val webView = rootView.findViewById<WebView>(R.id.readme_webview)
        when (readMeResult) {
            is Result.Ok -> {
                val content = STYLESHEET + readMeResult.value.string()
                webView.loadDataWithBaseURL("https://github.com", content, "text/html", "UTF-8", null)
            }
            is Result.Error -> {
                if (readMeResult.response.code() == 404)
                    webView.loadData(STYLESHEET + EMPTY_README, "text/html", "UTF-8")
                else
                    showErrorOnSnackbar(rootView, readMeResult.response.message())
            }
            is Result.Exception -> TODO()
        }

        rootView.isRefreshing = false
    }
}