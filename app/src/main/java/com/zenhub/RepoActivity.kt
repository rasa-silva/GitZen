package com.zenhub

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.webkit.WebView
import android.widget.TextView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RepoActivity : BaseActivity() {

    private val repoDetailsCallback = OnRepoDetailsResponse(this)
    private val readmeCallback = OnReadmeResponse(this)
    lateinit var fullRepoName: String
    lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_activity)
        super.onCreateDrawer()

        fullRepoName = intent.getStringExtra("REPO_FULL_NAME")

        //Fix the fight between the refreshLayout swipe and the webview scroll
        refreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        val webView = findViewById<WebView>(R.id.webview)
        refreshLayout.viewTreeObserver.addOnScrollChangedListener {
            refreshLayout.isEnabled = webView.scrollY == 0
        }

        requestDataRefresh()
    }


    override fun requestDataRefresh() {
        Log.d("ZenHub", "Refreshing repo information...")
        gitHubService.repoDetails(fullRepoName).enqueue(repoDetailsCallback)
        gitHubServiceRaw.repoReadme(fullRepoName).enqueue(readmeCallback)
    }
}

class OnRepoDetailsResponse(val activity: RepoActivity) : Callback<RepositoryDetails> {
    override fun onFailure(call: Call<RepositoryDetails>?, t: Throwable?) {
        Log.d("ZenHub", "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<RepositoryDetails>, response: Response<RepositoryDetails>) {
        Log.d("ZenHub", "RepoDetails reponse")
        //TODO Deal with non 200OK response
        activity.findViewById<TextView>(R.id.fullName).text = response.body()?.full_name
    }
}

class OnReadmeResponse(val activity: RepoActivity) : Callback<ResponseBody> {

    val styleSheet = "<style>body{color: #fff; background-color: #000;}</style>"

    override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
        Log.d("ZenHub", "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        Log.d("ZenHub", "readme reponse")
        //TODO Deal with non 200OK response
        val webView = activity.findViewById<WebView>(R.id.webview)
        val content = styleSheet + response.body()?.string()
        webView.loadData(content, "text/html", null)

        activity.refreshLayout.isRefreshing = false
    }

}