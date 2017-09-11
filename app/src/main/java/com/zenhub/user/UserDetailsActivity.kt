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
import com.zenhub.*
import com.zenhub.auth.LoggedUser
import com.zenhub.core.BaseActivity
import com.zenhub.core.PagedRecyclerViewAdapter
import com.zenhub.core.asFuzzyDate
import com.zenhub.gist.GistListActivity
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.*
import kotlinx.android.synthetic.main.zenhub_content.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResponse
import ru.gildor.coroutines.retrofit.awaitResult

class UserDetailsActivity : BaseActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val adapter by lazy { EventListAdapter(this) }
    private val user by lazy { intent.getStringExtra("USER") ?: LoggedUser.account?.name.orEmpty() }

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

        if (user == LoggedUser.account?.name) follow_switch.visibility = View.GONE

        repos_value.setOnClickListener {
            val intent = Intent(this, RepoListActivity::class.java)
            intent.putExtra("LIST_TYPE", RepoListType.OWN).putExtra("USER", user)
            startActivity(intent)
        }

        followers_value.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("LIST_TYPE", UserListType.FOLLOWERS).putExtra("USER", user)
            startActivity(intent)
        }

        following_value.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("LIST_TYPE", UserListType.FOLLOWING).putExtra("USER", user)
            startActivity(intent)
        }

        gists_value.setOnClickListener {
            val intent = Intent(this, GistListActivity::class.java)
            intent.putExtra("USER", user)
            startActivity(intent)
        }

        requestDataRefresh()
    }

    override fun requestDataRefresh() {

        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing user details...")
            val progressBar = findViewById<FrameLayout>(R.id.progress_overlay)
            progressBar.visibility = View.VISIBLE
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

            LoggedUser.account?.let {
                val result = gitHubService.userDetails(it.name).awaitResult()
                if (result is Result.Ok) {
                    val navDrawerAvatar = drawerLayout.findViewById<ImageView>(R.id.nav_avatar)
                    Application.picasso.load(result.value.avatar_url).transform(RoundedTransformation).into(navDrawerAvatar)
                    drawerLayout.findViewById<TextView>(R.id.nav_user).text = result.value.login
                }
            }

            val userDetails = gitHubService.userDetails(user).awaitResult()
            when (userDetails) {
                is Result.Ok -> {
                    val user = userDetails.value
                    val avatarView = drawerLayout.findViewById<ImageView>(R.id.avatar)
                    Application.picasso.load(user.avatar_url).transform(RoundedTransformation).into(avatarView)
                    drawerLayout.findViewById<TextView>(R.id.userid).text = user.login
                    drawerLayout.findViewById<TextView>(R.id.username).text = user.name
                    val joined = avatarView.resources.getString(R.string.user_joined, user.created_at.asFuzzyDate())
                    drawerLayout.findViewById<TextView>(R.id.joined).text = joined
                    val totalRepos = user.public_repos + user.total_private_repos
                    drawerLayout.findViewById<TextView>(R.id.repos_value).text = totalRepos.toString()
                    drawerLayout.findViewById<TextView>(R.id.followers_value).text = user.followers.toString()
                    drawerLayout.findViewById<TextView>(R.id.following_value).text = user.following.toString()
                    val gists = user.public_gists + (user.private_gists ?: 0)
                    drawerLayout.findViewById<TextView>(R.id.gists_value).text = gists.toString()
                }
                is Result.Error -> showErrorOnSnackbar(drawerLayout, userDetails.response.message())
                is Result.Exception -> Log.d(Application.LOGTAG, "Failed events call", userDetails.exception)
            }

            val isFollowing = gitHubService.isFollowing(user).awaitResponse()
            when {
                isFollowing.isSuccessful -> follow_switch.isChecked = true
                isFollowing.code() == 404 -> follow_switch.isChecked = false
                else -> showErrorOnSnackbar(follow_switch, isFollowing.message())
            }

            follow_switch.setOnCheckedChangeListener { button, isChecked ->
                launch(UI) {
                    if (isChecked) {
                        val response = gitHubService.follow(user).awaitResponse()
                        if (response.isSuccessful) showInfoOnSnackbar(button, "Started following $user")
                        else showErrorOnSnackbar(button, response.message())
                    } else {
                        val response = gitHubService.unfollow(user).awaitResponse()
                        if (response.isSuccessful) showInfoOnSnackbar(button, "Stopped following $user")
                        else showErrorOnSnackbar(button, response.message())
                    }
                }
            }

            val events = gitHubService.receivedEvents(user).awaitResult()
            when (events) {
                is Result.Ok -> adapter.updateDataSet(events)
                is Result.Error -> showErrorOnSnackbar(drawerLayout, events.response.message())
                is Result.Exception -> Log.d(Application.LOGTAG, "Failed events call", events.exception)
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
                .transform(RoundedTransformation)
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
            is CreateEvent -> {
                msg.text = msg.resources.getString(R.string.create_event,
                        event.payload.ref_type, event.payload.ref, event.repo)
            }
            is DeleteEvent -> {
                msg.text = msg.resources.getString(R.string.delete_event,
                        event.payload.ref_type, event.payload.ref, event.repo)
            }
            is IssueCommentEvent -> {
                msg.text = msg.resources.getString(R.string.issue_comment,
                        event.payload.action, event.payload.number, event.repo)
            }
            is PushEvent -> {
                msg.text = msg.resources.getString(R.string.push_event,
                        event.payload.ref, event.repo)
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