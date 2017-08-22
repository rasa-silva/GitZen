package com.zenhub.repo.commits

import android.graphics.Color
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.BaseActivity
import com.zenhub.R
import com.zenhub.github.CommitFile
import com.zenhub.github.GitHubApi

class RepoCommitDetails : BaseActivity() {

    private lateinit var repoName: String
    private lateinit var commitSha: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_commit_activity)
        super.onCreateDrawer()

        repoName = intent.getStringExtra("REPO_FULL_NAME")
        commitSha = intent.getStringExtra("COMMIT_SHA")

        val recyclerViewAdapter = CommitFilesRecyclerViewAdapter()
        findViewById<RecyclerView>(R.id.files).let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = recyclerViewAdapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        requestDataRefresh()
    }

    override fun requestDataRefresh() {
        Log.d(Application.LOGTAG, "Refreshing commit details...")
        val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        GitHubApi.commitDetails(repoName, commitSha, refreshLayout) { response, _ ->
            response?.let {
                val message = refreshLayout.findViewById<TextView>(R.id.commit_message)
                message.text = response.commit.message
                val sha = refreshLayout.findViewById<TextView>(R.id.commit_sha)
                sha.text = commitSha
                refreshLayout.findViewById<TextView>(R.id.commiter_name).text = response.commit.committer.name
                refreshLayout.findViewById<TextView>(R.id.changed_files).text = response.stats.total.toString()

                val adapter = refreshLayout.findViewById<RecyclerView>(R.id.files).adapter as CommitFilesRecyclerViewAdapter
                adapter.updateDataSet(response.files)
            }
            refreshLayout.isRefreshing = false
        }
    }
}

class CommitFilesRecyclerViewAdapter : RecyclerView.Adapter<CommitFilesRecyclerViewAdapter.ViewHolder>() {

    private val dataSet = mutableListOf<CommitFile>()

    fun updateDataSet(newDataSet: List<CommitFile>) {
        dataSet.clear()
        dataSet.addAll(newDataSet)
        notifyDataSetChanged()
    }

    override fun getItemCount() = dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_content_commit_item, parent, false)
        return CommitFilesRecyclerViewAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filePatch = dataSet[position]
        holder.itemView.findViewById<TextView>(R.id.filename).text = filePatch.filename

        val builder = SpannableStringBuilder()
        val deletedSpan = ForegroundColorSpan(Color.RED)
        val addedSpan = ForegroundColorSpan(Color.GREEN)
        filePatch.patch.lines().forEach {
            when {
                it.startsWith('+') -> builder.append(it, addedSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                it.startsWith('-') -> builder.append(it, deletedSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                else -> builder.append(it)
            }
            builder.append("\n")
        }

        holder.itemView.findViewById<TextView>(R.id.patch).text = builder
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}