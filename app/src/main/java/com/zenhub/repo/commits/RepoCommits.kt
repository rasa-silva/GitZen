package com.zenhub.repo.commits

import android.content.Context
import android.content.Intent
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
import android.widget.ImageView
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.RoundedTransformation
import com.zenhub.github.Commit
import com.zenhub.github.dateFormat
import com.zenhub.github.gitHubService
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

fun buildCommitsView(inflater: LayoutInflater, container: ViewGroup, fullRepoName: String): View {
    val view = inflater.inflate(R.layout.repo_content_commits, container, false)
    val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.commits_swiperefresh)
    val recyclerViewAdapter = CommitsRecyclerViewAdapter(fullRepoName)
    refreshLayout?.setOnRefreshListener { requestData(fullRepoName, container, recyclerViewAdapter) }

    view.findViewById<RecyclerView>(R.id.list).let {
        val layoutManager = LinearLayoutManager(it.context)
        it.layoutManager = layoutManager
        it.adapter = recyclerViewAdapter
        it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
    }

    requestData(fullRepoName, container, recyclerViewAdapter)

    return view
}

private fun requestData(fullRepoName: String, container: ViewGroup, adapter: CommitsRecyclerViewAdapter) {
    launch(UI) {
        Log.d(Application.LOGTAG, "Refreshing repo commits...")
        val result = gitHubService.commits(fullRepoName).awaitResult()
        when (result) {
            is Result.Ok -> adapter.updateDataSet(result.value)
            is Result.Error -> TODO()
            is Result.Exception -> TODO()
        }

        container.findViewById<SwipeRefreshLayout>(R.id.commits_swiperefresh).isRefreshing = false
    }
}

class CommitsRecyclerViewAdapter(private val repoName: String) : RecyclerView.Adapter<CommitsRecyclerViewAdapter.ViewHolder>() {

    private val dataSet = mutableListOf<Commit>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_content_commits_item, parent, false)
        return ViewHolder(parent.context, view, repoName)
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
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun updateDataSet(newDataSet: List<Commit>) {
        dataSet.clear()
        dataSet.addAll(newDataSet)
        notifyDataSetChanged()
    }

    class ViewHolder(ctx: Context, itemView: View, repoName: String) : RecyclerView.ViewHolder(itemView) {

        var sha: String = ""

        init {
            itemView.setOnClickListener {
                val intent = Intent(ctx, RepoCommitDetails::class.java)
                intent.putExtra("REPO_FULL_NAME", repoName)
                intent.putExtra("COMMIT_SHA", sha)
                ContextCompat.startActivity(ctx, intent, null)
            }
        }
    }
}
