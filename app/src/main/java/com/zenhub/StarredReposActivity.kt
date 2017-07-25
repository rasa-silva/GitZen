package com.zenhub

import android.content.Context
import android.os.Bundle
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
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

val dataSet = mutableListOf<Repository>()


class StarredReposActivity : BaseActivity() {

    val onResponseCallback = OnResponse(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.starred_repos_activity)
        super.onCreateDrawer()

        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = TestRecyclerViewAdapter()
        recyclerView.addItemDecoration(DividerItemDecoration(recyclerView.context, layoutManager.orientation))

        getStarredRepos(onResponseCallback)
    }

    override fun requestDataRefresh() {
        Log.d("XXX", "Refreshing list due to swipe")
        getStarredRepos(onResponseCallback)
    }
}

class TestRecyclerViewAdapter : RecyclerView.Adapter<TestRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.starred_repos_item, parent, false)
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


    class ViewHolder(ctx: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val textView = itemView.findViewById<TextView>(R.id.repo_full_name)
                Toast.makeText(ctx, "Clicked on ${textView.text}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class OnResponse(val activity: StarredReposActivity) : Callback<List<Repository>> {
    override fun onFailure(call: Call<List<Repository>>?, t: Throwable?) {
        Log.d("XXX", "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<List<Repository>>?, response: Response<List<Repository>>?) {
        Log.d("XXX", "Starred size: ${response?.body()?.size}")
        dataSet.clear()
        response?.body()?.let { dataSet.addAll(it) }
        val recyclerView = activity.findViewById<RecyclerView>(R.id.list)
        recyclerView.adapter.notifyDataSetChanged()
        val refreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        refreshLayout.isRefreshing = false
    }
}