package com.zenhub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.zenhub.repodetails.RepoActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StarredReposActivity : RepoListActivity() {

    val repoListCallback = OnRepoListResponse(adapter, this)

    override fun requestDataRefresh() {
        Log.d(Application.LOGTAG, "Refreshing list...")
        gitHubService.listStarred(repoListCallback.etag, STUBBED_USER).enqueue(repoListCallback)
    }
}

class OwnReposActivity : RepoListActivity() {

    val repoListCallback = OnRepoListResponse(adapter, this)

    override fun requestDataRefresh() {
        Log.d(Application.LOGTAG, "Refreshing list...")
        gitHubService.listRepos(repoListCallback.etag, STUBBED_USER).enqueue(repoListCallback)
    }
}

abstract class RepoListActivity : BaseActivity() {

    val adapter = RepoListRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_list_activity)
        super.onCreateDrawer()

        findViewById<RecyclerView>(R.id.list).let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        requestDataRefresh()
    }

    abstract override fun requestDataRefresh()
}

class RepoListRecyclerViewAdapter : RecyclerView.Adapter<RepoListRecyclerViewAdapter.ViewHolder>() {

    val dataSet = mutableListOf<Repository>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_list_item, parent, false)
        return ViewHolder(parent.context, view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val starredRepo = dataSet[position]
        val date = dateFormat.parse(starredRepo.pushed_at)
        val fuzzy_date = DateUtils.getRelativeTimeSpanString(date.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
        holder.itemView.findViewById<TextView>(R.id.repo_full_name).text = starredRepo.full_name
        holder.itemView.findViewById<TextView>(R.id.repo_description).text = starredRepo.description
        holder.itemView.findViewById<TextView>(R.id.repo_pushed_time).text = fuzzy_date
        holder.itemView.findViewById<TextView>(R.id.repo_stars).text = starredRepo.stargazers_count.toString()
        holder.itemView.findViewById<TextView>(R.id.repo_language).text = starredRepo.language
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun updateDataSet(newDataSet: List<Repository>) {
        dataSet.clear()
        dataSet.addAll(newDataSet)
        notifyDataSetChanged()
    }

    class ViewHolder(ctx: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val textView = itemView.findViewById<TextView>(R.id.repo_full_name)
                val intent = Intent(ctx, RepoActivity::class.java)
                intent.putExtra("REPO_FULL_NAME", textView.text.toString())
                ContextCompat.startActivity(ctx, intent, null)
            }
        }
    }
}

class OnRepoListResponse(val adapter: RepoListRecyclerViewAdapter,
                         val activity: RepoListActivity) : Callback<List<Repository>> {

    var etag: String? = null

    override fun onFailure(call: Call<List<Repository>>?, t: Throwable?) {
        Log.d(Application.LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<List<Repository>>?, response: Response<List<Repository>>) {
        Log.d(Application.LOGTAG, "Repo list size: ${response.body()?.size}")
        val refreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        when {
            response.code() == 304 -> Unit
            !response.isSuccessful -> showGitHubApiError(response.errorBody(), refreshLayout)
            else -> {
                etag = response.headers()["ETag"]
                response.body()?.let { adapter.updateDataSet(it) }
            }
        }

        refreshLayout.isRefreshing = false
    }
}