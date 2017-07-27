package com.zenhub

import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_activity)
        super.onCreateDrawer()

        fullRepoName = intent.getStringExtra("REPO_FULL_NAME")
        supportActionBar?.title = fullRepoName

        val viewPager = findViewById<ViewPager>(R.id.pager)
        viewPager.adapter = RepoDetailsPagerAdapter(this)
        val tabLayout = findViewById<TabLayout>(R.id.tablayout)
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position, true)
            }
        })

        requestDataRefresh()
    }

    override fun requestDataRefresh() {
        Log.d("ZenHub", "Refreshing repo information...")
        gitHubService.repoDetails(fullRepoName).enqueue(repoDetailsCallback)
        gitHubServiceRaw.repoReadme(fullRepoName).enqueue(readmeCallback)
    }
}

class RepoDetailsPagerAdapter(context: Context) : PagerAdapter() {

    val inflater = LayoutInflater.from(context)

    override fun isViewFromObject(view: View?, obj: Any?): Boolean {
        Log.d("ZenHub", "isViewFromObject called")
        return view == obj
    }

    override fun getCount(): Int {
        return 3
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = when (position) {
            0 -> {
                val view = inflater.inflate(R.layout.repo_content_readme, container, false)
                //Fix the fight between the refreshLayout swipe and the webview scroll
                //TODO NestedScrollView?
                val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
                val webView = view.findViewById<WebView>(R.id.webview)
                refreshLayout.viewTreeObserver.addOnScrollChangedListener {
                    refreshLayout.isEnabled = webView.scrollY == 0
                }
                view
            }
            else -> inflater.inflate(R.layout.repo_content, container, false)
        }
        container.addView(layout)
        return layout
    }

    override fun destroyItem(container: ViewGroup?, position: Int, view: Any?) {
        container?.removeView(view as View?)
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

        val refreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        refreshLayout?.isRefreshing = false
    }

}