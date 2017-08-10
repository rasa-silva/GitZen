package com.zenhub.repodetails

import android.content.Context
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
import android.widget.Toast
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.RoundedTransformation
import com.zenhub.github.Commit
import com.zenhub.github.GitHubApi
import com.zenhub.github.dateFormat

fun buildCommitsView(inflater: LayoutInflater, container: ViewGroup, fullRepoName: String): View {
    val view = inflater.inflate(R.layout.repo_content_commits, container, false)
    val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.commits_swiperefresh)
    val recyclerViewAdapter = CommitsRecyclerViewAdapter()
    refreshLayout?.setOnRefreshListener { requestData(fullRepoName, container, recyclerViewAdapter) }

    view.findViewById<RecyclerView>(R.id.list).let {
        val layoutManager = LinearLayoutManager(it.context)
        it.layoutManager = layoutManager
        it.adapter = recyclerViewAdapter
        it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
    }

    requestData(fullRepoName, container,recyclerViewAdapter )

    return view
}

private fun requestData(fullRepoName: String, container: ViewGroup, adapter: CommitsRecyclerViewAdapter) {
    Log.d(Application.LOGTAG, "Refreshing repo commits...")
    GitHubApi.commits(fullRepoName, container) { response, parentView ->
        response?.let { adapter.updateDataSet(it) }
        parentView.findViewById<SwipeRefreshLayout>(R.id.commits_swiperefresh).isRefreshing = false
    }
}

class CommitsRecyclerViewAdapter : RecyclerView.Adapter<CommitsRecyclerViewAdapter.ViewHolder>() {

    val dataSet = mutableListOf<Commit>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_content_commits_item, parent, false)
        return ViewHolder(parent.context, view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val commit = dataSet[position]

        holder.sha = commit.sha

        val avatarView = holder.itemView.findViewById<ImageView>(R.id.avatar)
        commit.committer?.let {
            Application.picasso.load(it.avatar_url)
                    .transform(RoundedTransformation()).into(avatarView)
        }

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

        var sha: String = ""

        init {
            itemView.setOnClickListener {
                Toast.makeText(ctx, "Will show commit $sha", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
