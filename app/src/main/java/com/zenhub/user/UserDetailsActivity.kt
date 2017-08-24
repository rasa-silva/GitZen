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
import com.zenhub.*
import com.zenhub.github.STUBBED_USER
import com.zenhub.github.dateFormat
import com.zenhub.github.gitHubService
import com.zenhub.lists.RepoListRecyclerViewAdapter
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

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
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing list...")
            val progressBar = findViewById<FrameLayout>(R.id.progress_overlay)
            progressBar.visibility = View.VISIBLE
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

            val userDetails = gitHubService.userDetails(STUBBED_USER).awaitResult()
            when (userDetails) {
                is Result.Ok -> {
                    val user = userDetails.value
                    val avatarView = drawerLayout.findViewById<ImageView>(R.id.avatar)
                    val roundedTransformation = RoundedTransformation()
                    Application.picasso.load(user.avatar_url).transform(roundedTransformation).into(avatarView)
                    val navDrawerAvatar = drawerLayout.findViewById<ImageView>(R.id.avatarImage)
                    Application.picasso.load(user.avatar_url).transform(roundedTransformation).into(navDrawerAvatar)
                    drawerLayout.findViewById<TextView>(R.id.userid).text = user.login
                    drawerLayout.findViewById<TextView>(R.id.username).text = user.name
                    val created = drawerLayout.findViewById<TextView>(R.id.created_at)
                    val date_created = dateFormat.parse(user.created_at)
                    created.text = DateUtils.formatDateTime(drawerLayout.context, date_created.time, DateUtils.FORMAT_SHOW_DATE)
                    val followers = drawerLayout.findViewById<TextView>(R.id.followers)
                    followers.text = drawerLayout.resources.getString(R.string.numberOfFollowers, user.followers)
                    val following = drawerLayout.findViewById<TextView>(R.id.following)
                    following.text = drawerLayout.resources.getString(R.string.numberOfFollowing, user.following)
                    val gists = drawerLayout.findViewById<TextView>(R.id.gists)
                    gists.text = drawerLayout.resources.getString(R.string.numberOfGists, user.public_gists)
                }
                is Result.Error -> showErrorOnSnackbar(drawerLayout, userDetails.response.message())
                is Result.Exception -> TODO()
            }

            val reposResponse = gitHubService.listRepos(STUBBED_USER).awaitResult()
            when (reposResponse) {
                is Result.Ok -> {
                    val repos = reposResponse.value
                    val top3Repos = repos.sortedByDescending { it.pushed_at }.take(3)
                    adapter.updateDataSet(top3Repos)
                }
                is Result.Error -> showErrorOnSnackbar(drawerLayout, reposResponse.response.message())
                is Result.Exception -> TODO()
            }

            val refreshLayout = drawerLayout.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
            refreshLayout.isRefreshing = false
            progressBar.visibility = View.GONE
        }
    }
}

