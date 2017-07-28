package com.zenhub.repodetails

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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

class RepoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_activity)
        super.onCreateDrawer()

        val fullRepoName = intent.getStringExtra("REPO_FULL_NAME")
        supportActionBar?.title = fullRepoName

        val viewPager = findViewById<ViewPager>(R.id.pager)
        viewPager.adapter = RepoDetailsPagerAdapter(this, fullRepoName)
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
}

class RepoDetailsPagerAdapter(context: Activity, val fullRepoName: String) : PagerAdapter() {

    val inflater = LayoutInflater.from(context)

    override fun isViewFromObject(view: View?, obj: Any?) = view == obj

    override fun getCount() = 3

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = when (position) {
            0 -> {
                val view = inflater.inflate(R.layout.repo_content_readme, container, false)
                //Fix the fight between the refreshLayout swipe and the webview scroll
                //TODO NestedScrollView?
                val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.readme_swiperefresh)
                val webView = view.findViewById<WebView>(R.id.readme_webview)
                refreshLayout.viewTreeObserver.addOnScrollChangedListener {
                    refreshLayout.isEnabled = webView.scrollY == 0
                }

                //We have several swiperefresh layouts on this activity so we need to do this for each
                val onRepoDetailsResponse = OnRepoDetailsResponse(container)
                val onReadmeResponse = OnReadmeResponse(container)
                refreshLayout?.setOnRefreshListener {
                    Log.d(LOGTAG, "Refreshing repo information...")
                    gitHubService.repoDetails(fullRepoName).enqueue(onRepoDetailsResponse)
                    gitHubServiceRaw.repoReadme(fullRepoName).enqueue(onReadmeResponse)
                }

                gitHubService.repoDetails(fullRepoName).enqueue(onRepoDetailsResponse)
                gitHubServiceRaw.repoReadme(fullRepoName).enqueue(onReadmeResponse)

                view
            }
            1 -> {
                val view = inflater.inflate(R.layout.repo_content_commits, container, false)
                val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.commits_swiperefresh)
                val recyclerViewAdapter = CommitsRecyclerViewAdapter()
                val onCommitsResponse = OnCommitsResponse(recyclerViewAdapter, container)
                refreshLayout?.setOnRefreshListener {
                    Log.d(LOGTAG, "Refreshing repo information...")
                    gitHubService.commits(fullRepoName).enqueue(onCommitsResponse)
                }

                view.findViewById<RecyclerView>(R.id.list).let {
                    val layoutManager = LinearLayoutManager(it.context)
                    it.layoutManager = layoutManager
                    it.adapter = recyclerViewAdapter
                    it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
                }

                gitHubService.commits(fullRepoName).enqueue(onCommitsResponse)

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

class OnRepoDetailsResponse(val parent: ViewGroup) : Callback<RepositoryDetails> {
    override fun onFailure(call: Call<RepositoryDetails>?, t: Throwable?) {
        Log.d(LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<RepositoryDetails>, response: Response<RepositoryDetails>) {
        Log.d(LOGTAG, "RepoDetails reponse")
        //TODO Deal with non 200OK response
        parent.findViewById<TextView>(R.id.fullName).text = response.body()?.full_name
    }
}

class OnReadmeResponse(val parent: ViewGroup) : Callback<ResponseBody> {

    val styleSheet = """
    <style>
        body {color: #ffffff; background-color: #303030;}
        a {color: #3f51b5;}
        pre {overflow: auto; width: 95%;}
    </style>"""

    override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
        Log.d(LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        Log.d(LOGTAG, "readme reponse")
        //TODO Deal with non 200OK response
        val webView = parent.findViewById<WebView>(R.id.readme_webview)
        val content = styleSheet + response.body()?.string()
        webView.loadDataWithBaseURL("https://github.com", content, "text/html", "UTF-8", null)

        val refreshLayout = parent.findViewById<SwipeRefreshLayout>(R.id.readme_swiperefresh)
        refreshLayout.isRefreshing = false
    }
}