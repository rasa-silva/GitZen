package com.zenhub.repodetails

import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import com.zenhub.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun buildReadmeView(inflater: LayoutInflater, container: ViewGroup, fullRepoName: String): View {
    val view = inflater.inflate(R.layout.repo_content_readme, container, false)
    //Fix the fight between the refreshLayout swipe and the webview scroll
    //TODO NestedScrollView?
    val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.readme_swiperefresh)
    val webView = view.findViewById<WebView>(R.id.readme_webview)
    refreshLayout.viewTreeObserver.addOnScrollChangedListener {
        refreshLayout.isEnabled = webView.scrollY == 0
    }

    val onRepoDetailsResponse = OnRepoDetailsResponse(container)
    val onReadmeResponse = OnReadmeResponse(container)
    refreshLayout?.setOnRefreshListener {
        Log.d(Application.LOGTAG, "Refreshing repo information...")
        gitHubService.repoDetails(fullRepoName).enqueue(onRepoDetailsResponse)
        gitHubServiceRaw.repoReadme(fullRepoName).enqueue(onReadmeResponse)
    }

    gitHubService.repoDetails(fullRepoName).enqueue(onRepoDetailsResponse)
    gitHubServiceRaw.repoReadme(fullRepoName).enqueue(onReadmeResponse)

    return view
}

class OnRepoDetailsResponse(val parent: ViewGroup) : Callback<RepositoryDetails> {
    override fun onFailure(call: Call<RepositoryDetails>?, t: Throwable?) {
        Log.d(Application.LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<RepositoryDetails>, response: Response<RepositoryDetails>) {
        Log.d(Application.LOGTAG, "RepoDetails reponse")
        //TODO Deal with non 200OK response
        parent.findViewById<TextView>(R.id.fullName).text = response.body()?.full_name
    }
}

class OnReadmeResponse(val parent: ViewGroup) : Callback<ResponseBody> {

    val styleSheet = """
    <style>
        body {color: #ffffff; background-color: #303030;}
        a {color: #3f51b5;}
        pre {overflow: auto; width: 99%; background-color: #424242;}
    </style>"""

    override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
        Log.d(Application.LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        Log.d(Application.LOGTAG, "readme reponse")
        if (!response.isSuccessful)
            showGitHubApiError(response.errorBody(), parent)
        else {
            val webView = parent.findViewById<WebView>(R.id.readme_webview)
            val content = styleSheet + response.body()?.string()
            webView.loadDataWithBaseURL("https://github.com", content, "text/html", "UTF-8", null)
        }

        parent.findViewById<SwipeRefreshLayout>(R.id.readme_swiperefresh).isRefreshing = false
    }
}