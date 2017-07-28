package com.zenhub

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.text.format.DateUtils
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

const val LOGTAG = "ZenHub"

class ZenHub : BaseActivity() {

    val userDetailsCallback = OnUserDetailsResponse(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.zenhub_activity)
        super.onCreateDrawer()

        requestDataRefresh()
    }

    override fun requestDataRefresh() {
        Log.d(LOGTAG, "Refreshing list...")
        gitHubService.userDetails(STUBBED_USER).enqueue(userDetailsCallback)
    }
}

class OnUserDetailsResponse(val activity: ZenHub) : Callback<User> {

    override fun onFailure(call: Call<User>?, t: Throwable?) {
        Log.d(LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<User>?, response: Response<User>?) {
        Log.d(LOGTAG, "UserDetails reponse")

        if (response?.isSuccessful == false) {
            val layout = activity.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
            val snackbar = Snackbar.make(layout, "Failed: ${response.errorBody()?.string()}.", Snackbar.LENGTH_LONG)
            snackbar.show()
            return
        }

        val picasso = Picasso.with(activity.applicationContext)
        picasso.setIndicatorsEnabled(true)
        val avatarView = activity.findViewById<ImageView>(R.id.avatar)
        picasso.load(response?.body()?.avatar_url).into(avatarView)
        val login = activity.findViewById<TextView>(R.id.userid)
        login.text = response?.body()?.login
        val name = activity.findViewById<TextView>(R.id.username)
        name.text = response?.body()?.name
        val created = activity.findViewById<TextView>(R.id.created_at)
        val date_created = dateFormat.parse(response?.body()?.created_at)
        created.text = DateUtils.formatDateTime(activity.applicationContext, date_created.time, DateUtils.FORMAT_SHOW_DATE)
        val followers = activity.findViewById<TextView>(R.id.followers)
        followers.text = activity.resources.getString(R.string.numberOfFollowers, response?.body()?.followers)
        val following = activity.findViewById<TextView>(R.id.following)
        following.text = activity.resources.getString(R.string.numberOfFollowing, response?.body()?.following)
        val gists = activity.findViewById<TextView>(R.id.gists)
        gists.text = activity.resources.getString(R.string.numberOfGists, response?.body()?.public_gists)
        val repos = activity.findViewById<TextView>(R.id.repo_count)
        repos.text = response?.body()?.public_repos.toString()

        val refreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        refreshLayout.isRefreshing = false
    }
}
