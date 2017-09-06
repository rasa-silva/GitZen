package com.zenhub.user

import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.RoundedTransformation
import com.zenhub.auth.LoggedUser
import com.zenhub.core.BaseActivity
import com.zenhub.core.PagedRecyclerViewAdapter
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.*
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class UserDetailsActivity : BaseActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val adapter by lazy { EventListAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.zenhub_activity)
        super.onCreateDrawer()

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        findViewById<TextView>(R.id.repos_value).setOnClickListener {
            val intent = Intent(this, RepoListActivity::class.java)
            startActivity(intent.putExtra("LIST_TYPE", REPO_LIST_TYPE.OWN))
        }

        requestDataRefresh()
    }

    override fun requestDataRefresh() {

        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing user details...")
            val progressBar = findViewById<FrameLayout>(R.id.progress_overlay)
            progressBar.visibility = View.VISIBLE
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

            val userDetails = gitHubService.userDetails().awaitResult()
            when (userDetails) {
                is Result.Ok -> {
                    val user = userDetails.value
                    val avatarView = drawerLayout.findViewById<ImageView>(R.id.avatar)
                    val roundedTransformation = RoundedTransformation()
                    Application.picasso.load(user.avatar_url).transform(roundedTransformation).into(avatarView)
                    val navDrawerAvatar = drawerLayout.findViewById<ImageView>(R.id.nav_avatar)
                    Application.picasso.load(user.avatar_url).transform(roundedTransformation).into(navDrawerAvatar)
                    drawerLayout.findViewById<TextView>(R.id.userid).text = user.login
                    drawerLayout.findViewById<TextView>(R.id.nav_user).text = user.login
                    drawerLayout.findViewById<TextView>(R.id.username).text = user.name
                    val joined = avatarView.resources.getString(R.string.user_joined, user.created_at.asFuzzyDate())
                    drawerLayout.findViewById<TextView>(R.id.joined).text = joined
                    val totalRepos = user.public_repos + user.total_private_repos
                    drawerLayout.findViewById<TextView>(R.id.repos_value).text = totalRepos.toString()
                    drawerLayout.findViewById<TextView>(R.id.followers_value).text = user.followers.toString()
                    drawerLayout.findViewById<TextView>(R.id.following_value).text = user.following.toString()
                    drawerLayout.findViewById<TextView>(R.id.gists_value).text = user.public_gists.toString()
                }
                is Result.Error -> showErrorOnSnackbar(drawerLayout, userDetails.response.message())
                is Result.Exception -> Log.d(Application.LOGTAG, "Failed events call", userDetails.exception)
            }

            LoggedUser.account?.name?.let {

                val events = gitHubService.receivedEvents(it).awaitResult()
                when (events) {
                    is Result.Ok -> adapter.updateDataSet(events)
                    is Result.Error -> showErrorOnSnackbar(drawerLayout, events.response.message())
                    is Result.Exception -> Log.d(Application.LOGTAG, "Failed events call", events.exception)
                }
            }

            drawerLayout.findViewById<SwipeRefreshLayout>(R.id.swiperefresh).isRefreshing = false
            progressBar.visibility = View.GONE
        }
    }
}

class EventListAdapter(activity: UserDetailsActivity) : PagedRecyclerViewAdapter<ReceivedEvent?>(activity, R.layout.user_events_item) {

    private val recyclerView = activity.findViewById<RecyclerView>(R.id.list)

    override fun bindData(itemView: View, model: ReceivedEvent?) {
        val event = model ?: return

        Application.picasso.load(model.actor.avatar_url)
                .transform(RoundedTransformation())
                .into(itemView.findViewById<ImageView>(R.id.avatar))
        itemView.findViewById<TextView>(R.id.actor).text = event.actor.display_login
        itemView.findViewById<TextView>(R.id.created_at).text = event.created_at.asFuzzyDate()

        val msg = itemView.findViewById<TextView>(R.id.message)
        when (event.payload) {
            is WatchEvent -> {
                msg.text = msg.resources.getString(R.string.watch_event, event.payload.action, event.repo)
            }
            is PullRequestEvent -> {
                msg.text = msg.resources.getString(R.string.pullrequest_event,
                        event.payload.action, event.payload.number, event.repo)
            }
            is IssuesEvent -> {
                msg.text = msg.resources.getString(R.string.issues_event,
                        event.payload.action, event.payload.number, event.repo)
            }
            ForkEvent -> {
                msg.text = msg.resources.getString(R.string.fork_event, event.repo)
            }
            UnsupportedEvent -> {
                msg.text = msg.resources.getString(R.string.unsupported_event, event.type)
            }
        }
    }

    override fun doPageRequest(url: String) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Paging user event list with $url...")
            val result = gitHubService.receivedEventsPaginate(url).awaitResult()
            when (result) {
                is Result.Ok -> appendDataSet(result.value)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}