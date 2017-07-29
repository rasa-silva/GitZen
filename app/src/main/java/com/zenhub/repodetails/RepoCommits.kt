package com.zenhub.repodetails

import android.content.Context
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.zenhub.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun buildCommitsView(inflater: LayoutInflater, container: ViewGroup, fullRepoName: String): View {
        val view = inflater.inflate(R.layout.repo_content_commits, container, false)
        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.commits_swiperefresh)
        val recyclerViewAdapter = CommitsRecyclerViewAdapter()
        val onCommitsResponse = OnCommitsResponse(recyclerViewAdapter, container)
        refreshLayout?.setOnRefreshListener {
            Log.d(Application.LOGTAG, "Refreshing repo information...")
            gitHubService.commits(fullRepoName).enqueue(onCommitsResponse)
        }

        view.findViewById<RecyclerView>(R.id.list).let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = recyclerViewAdapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        gitHubService.commits(fullRepoName).enqueue(onCommitsResponse)

        return view
}
class CommitsRecyclerViewAdapter : RecyclerView.Adapter<CommitsRecyclerViewAdapter.ViewHolder>() {

    val dataSet = mutableListOf<Commit>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_content_commits_item, parent, false)
        return ViewHolder(parent.context, view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val commit = dataSet[position]

        val avatarView = holder.itemView.findViewById<ImageView>(R.id.avatar)
        commit.committer?.let { Application.picasso.load(it.avatar_url)
                .transform(RoundedTransformation()).into(avatarView) }

        val date = dateFormat.parse(commit.commit.committer.date)
        val fuzzy_date = DateUtils.getRelativeTimeSpanString(date.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
        holder.itemView.findViewById<TextView>(R.id.commit_message).text = commit.commit.message
        holder.itemView.findViewById<TextView>(R.id.committer).text = commit.committer?.login ?: "<unknown>"
        holder.itemView.findViewById<TextView>(R.id.pushed_time).text = fuzzy_date
        holder.itemView.findViewById<TextView>(R.id.comments).text = commit.commit.comment_count.toString()
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun updateDataSet(newDataSet: List<Commit>) {
        dataSet.clear()
        dataSet.addAll(newDataSet)
        notifyDataSetChanged()
    }

    class ViewHolder(ctx: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
//        init {
//            itemView.setOnClickListener {
//                val textView = itemView.findViewById<TextView>(R.id.repo_full_name)
//                val intent = Intent(ctx, RepoActivity::class.java)
//                intent.putExtra("REPO_FULL_NAME", textView.text.toString())
//                ContextCompat.startActivity(ctx, intent, null)
//            }
//        }
    }
}


class OnCommitsResponse(val adapter: CommitsRecyclerViewAdapter,
                        val parent: ViewGroup) : Callback<List<Commit>> {
    override fun onFailure(call: Call<List<Commit>>?, t: Throwable?) {
        Log.d(Application.LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<List<Commit>>?, response: Response<List<Commit>>) {
        Log.d(Application.LOGTAG, "commits reponse")
        if (!response.isSuccessful) {
            val error = response.errorBody()?.string()
            Log.e(Application.LOGTAG, "Failed response: $error")
            val snackbar = Snackbar.make(parent, "Failed: $error.", Snackbar.LENGTH_INDEFINITE)
            snackbar.show()
            return
        }
        response.body()?.let { adapter.updateDataSet(it) }

        val refreshLayout = parent.findViewById<SwipeRefreshLayout>(R.id.commits_swiperefresh)
        refreshLayout.isRefreshing = false
    }

}