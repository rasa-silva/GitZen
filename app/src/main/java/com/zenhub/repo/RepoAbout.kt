package com.zenhub.repo

import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.content.res.AppCompatResources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.zenhub.*
import com.zenhub.core.asDigitalUnit
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.getLanguageColor
import com.zenhub.github.gitHubService
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResponse
import ru.gildor.coroutines.retrofit.awaitResult


private const val STYLESHEET = """
    <style>
        body {color: #ffffff; background-color: #424242;}
        a {color: #458588;}
        pre {overflow: auto; width: 99%; background-color: #424242;}
    </style>"""

private const val EMPTY_README = """<body><h1>No ReadMe available.</h1></body>"""

fun buildAboutView(inflater: LayoutInflater, container: ViewGroup, fullRepoName: String): View {
    val view = inflater.inflate(R.layout.repo_content_readme, container, false)
    //Fix the fight between the refreshLayout swipe and the webview scroll
    val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.readme_swiperefresh)
    val webView = view.findViewById<WebView>(R.id.readme_webview)
    refreshLayout.viewTreeObserver.addOnScrollChangedListener {
        refreshLayout.isEnabled = webView.scrollY == 0
    }

    refreshLayout?.setOnRefreshListener { requestAboutData(fullRepoName, refreshLayout) }
    requestAboutData(fullRepoName, refreshLayout)
    return view
}

private fun requestAboutData(fullRepoName: String, rootView: SwipeRefreshLayout) {
    launch(UI) {
        Log.d(Application.LOGTAG, "Refreshing repo information...")
        val progressBar = rootView.findViewById<FrameLayout>(R.id.progress_overlay)
        progressBar.visibility = View.VISIBLE

        val starredView = rootView.findViewById<ImageView>(R.id.starred)
        val isStarred = gitHubService.isStarred(fullRepoName).awaitResponse()
        when {
            isStarred.isSuccessful -> setAsStarred(starredView, fullRepoName)
            isStarred.code() == 404 -> setAsUnstarred(starredView, fullRepoName)
            else -> showErrorOnSnackbar(starredView, isStarred.message())
        }

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

                rootView.findViewById<TextView>(R.id.stargazers_count)?.text = details.stargazers_count.toString()
                rootView.findViewById<TextView>(R.id.size_value)?.text = (details.size * 1024).asDigitalUnit()
                rootView.findViewById<TextView>(R.id.pushed_time_value)?.text = details.pushed_at.asFuzzyDate()
            }
            is Result.Error -> showErrorOnSnackbar(rootView, repoDetails.response.message())
            is Result.Exception -> showExceptionOnSnackbar(rootView, repoDetails.exception)
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
            is Result.Exception -> showExceptionOnSnackbar(rootView, readMeResult.exception)
        }

        rootView.isRefreshing = false
        progressBar.visibility = View.GONE
    }
}

private fun setAsStarred(view: ImageView, fullRepoName: String) {
    val drawable = AppCompatResources.getDrawable(view.context, R.drawable.ic_star_white_24px)
    view.setImageDrawable(drawable)
    view.setOnClickListener {
        launch(UI) {
            val response = gitHubService.unstarRepo(fullRepoName).awaitResponse()
            if (response.isSuccessful) {
                showInfoOnSnackbar(view, "Unstarred $fullRepoName")
                setAsUnstarred(view, fullRepoName)
            } else {
                showErrorOnSnackbar(view, "Failed to unstar $fullRepoName")
            }
        }
    }
}

private fun setAsUnstarred(view: ImageView, fullRepoName: String) {
    val drawable = AppCompatResources.getDrawable(view.context, R.drawable.ic_star_border_white_24px)
    view.setImageDrawable(drawable)
    view.setOnClickListener {
        launch(UI) {
            val response = gitHubService.starRepo(fullRepoName).awaitResponse()
            if (response.isSuccessful) {
                showInfoOnSnackbar(view, "Starred $fullRepoName")
                setAsStarred(view, fullRepoName)
            } else {
                showErrorOnSnackbar(view, "Failed to star $fullRepoName")
            }
        }
    }
}
