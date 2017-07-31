package com.zenhub

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.text.format.DateUtils
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ZenHub : BaseActivity() {

    private val userDetailsCallback = OnUserDetailsResponse(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.zenhub_activity)
        super.onCreateDrawer()

        requestDataRefresh()
    }

    override fun requestDataRefresh() {
        Log.d(Application.LOGTAG, "Refreshing list...")
        gitHubService.userDetails(STUBBED_USER).enqueue(userDetailsCallback)
    }
}

class OnUserDetailsResponse(val activity: ZenHub) : Callback<User> {

    override fun onFailure(call: Call<User>?, t: Throwable?) {
        Log.d(Application.LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<User>?, response: Response<User>) {
        Log.d(Application.LOGTAG, "UserDetails reponse")
        val refreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)

        if (!response.isSuccessful) {
            showGitHubApiError(response.errorBody(), refreshLayout)
        } else {
            val responseBody = response.body()
            val avatarView = activity.findViewById<ImageView>(R.id.avatar)
            val roundedTransformation = RoundedTransformation()
            Application.picasso.load(responseBody?.avatar_url).transform(roundedTransformation).into(avatarView)
            val navDrawerAvatar = activity.findViewById<ImageView>(R.id.avatarImage)
            Application.picasso.load(responseBody?.avatar_url).transform(roundedTransformation).into(navDrawerAvatar)
            activity.findViewById<TextView>(R.id.userid).text = responseBody?.login
            activity.findViewById<TextView>(R.id.username).text = responseBody?.name
            val created = activity.findViewById<TextView>(R.id.created_at)
            val date_created = dateFormat.parse(responseBody?.created_at)
            created.text = DateUtils.formatDateTime(activity.applicationContext, date_created.time, DateUtils.FORMAT_SHOW_DATE)
            val followers = activity.findViewById<TextView>(R.id.followers)
            followers.text = activity.resources.getString(R.string.numberOfFollowers, responseBody?.followers)
            val following = activity.findViewById<TextView>(R.id.following)
            following.text = activity.resources.getString(R.string.numberOfFollowing, responseBody?.following)
            val gists = activity.findViewById<TextView>(R.id.gists)
            gists.text = activity.resources.getString(R.string.numberOfGists, responseBody?.public_gists)
            activity.findViewById<TextView>(R.id.repo_count).text = responseBody?.public_repos.toString()
        }

        refreshLayout.isRefreshing = false
    }
}
