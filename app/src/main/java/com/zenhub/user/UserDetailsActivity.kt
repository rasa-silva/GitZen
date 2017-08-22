package com.zenhub.user

import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.BaseActivity
import com.zenhub.R
import com.zenhub.RoundedTransformation
import com.zenhub.github.GitHubApi
import com.zenhub.github.STUBBED_USER
import com.zenhub.github.dateFormat
import com.zenhub.lists.RepoListRecyclerViewAdapter

class UserDetailsActivity : BaseActivity() {

    private val adapter = RepoListRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.zenhub_activity)
        super.onCreateDrawer()

        findViewById<RecyclerView>(R.id.repo_list).let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        requestDataRefresh()
    }

    override fun requestDataRefresh() {
        Log.d(Application.LOGTAG, "Refreshing list...")
        val progressBar = findViewById<FrameLayout>(R.id.progress_overlay)
        progressBar.visibility = View.VISIBLE
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        GitHubApi.userDetails(STUBBED_USER, drawerLayout) { response, rootView ->
            val avatarView = rootView.findViewById<ImageView>(R.id.avatar)
            val roundedTransformation = RoundedTransformation()
            Application.picasso.load(response?.avatar_url).transform(roundedTransformation).into(avatarView)
            val navDrawerAvatar = rootView.findViewById<ImageView>(R.id.avatarImage)
            Application.picasso.load(response?.avatar_url).transform(roundedTransformation).into(navDrawerAvatar)
            rootView.findViewById<TextView>(R.id.userid).text = response?.login
            rootView.findViewById<TextView>(R.id.username).text = response?.name
            val created = rootView.findViewById<TextView>(R.id.created_at)
            val date_created = dateFormat.parse(response?.created_at)
            created.text = DateUtils.formatDateTime(rootView.context, date_created.time, DateUtils.FORMAT_SHOW_DATE)
            val followers = rootView.findViewById<TextView>(R.id.followers)
            followers.text = rootView.resources.getString(R.string.numberOfFollowers, response?.followers)
            val following = rootView.findViewById<TextView>(R.id.following)
            following.text = rootView.resources.getString(R.string.numberOfFollowing, response?.following)
            val gists = rootView.findViewById<TextView>(R.id.gists)
            gists.text = rootView.resources.getString(R.string.numberOfGists, response?.public_gists)

            val refreshLayout = rootView.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
            refreshLayout.isRefreshing = false
            progressBar.visibility = View.GONE
        }

        val recyclerView = findViewById<RecyclerView>(R.id.repo_list)
        GitHubApi.ownRepos(recyclerView, { response, _ ->
            if (response == null) {
                Log.d(Application.LOGTAG, "Response is null. Will not update contents.")
            } else {
                val top3Repos = response.sortedByDescending { it.pushed_at }.take(3)
                adapter.updateDataSet(top3Repos)
            }
        })
    }
}

