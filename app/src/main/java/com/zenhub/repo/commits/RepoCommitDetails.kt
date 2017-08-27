package com.zenhub.repo.commits

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pddstudio.highlightjs.HighlightJsView
import com.pddstudio.highlightjs.models.Language
import com.pddstudio.highlightjs.models.Theme
import com.zenhub.Application
import com.zenhub.BaseActivity
import com.zenhub.R
import com.zenhub.github.CommitFile
import com.zenhub.github.gitHubService
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class RepoCommitDetails : BaseActivity() {

    private lateinit var repoName: String
    private lateinit var commitSha: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_commit_activity)
        super.onCreateDrawer()

        repoName = intent.getStringExtra("REPO_FULL_NAME")
        supportActionBar?.title = repoName

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
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing commit details...")
            val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
            val result = gitHubService.commit(repoName, commitSha).awaitResult()
            when (result) {
                is Result.Ok -> {
                    val response = result.value
                    val firstAndOtherLines = response.commit.message.split("\n\n")
                    val styledCommitMsg = if (firstAndOtherLines.size == 1) response.commit.message
                    else {
                        SpannableStringBuilder()
                                .append(firstAndOtherLines[0])
                                .append(firstAndOtherLines[1], RelativeSizeSpan(0.75f), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    refreshLayout.findViewById<TextView>(R.id.commit_message).text = styledCommitMsg

                    val commit_details_stats = refreshLayout.findViewById<TextView>(R.id.commit_details_stats)
                    val statsText = commit_details_stats.resources.getString(R.string.commit_details_stats, response.commit.committer.name, response.files.size)
                    commit_details_stats.text = statsText

                    val adapter = refreshLayout.findViewById<RecyclerView>(R.id.files).adapter as CommitFilesRecyclerViewAdapter
                    adapter.updateDataSet(response.files)
                }
                is Result.Error -> TODO()
                is Result.Exception -> TODO()
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

        val patchView = view.findViewById<HighlightJsView>(R.id.patch)
        with(patchView) {
            theme = Theme.TOMORROW_NIGHT_EIGHTIES
            highlightLanguage = Language.DIFF
            setZoomSupportEnabled(true)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        return CommitFilesRecyclerViewAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filePatch = dataSet[position]
        holder.itemView.findViewById<TextView>(R.id.filename).text = filePatch.filename
        holder.itemView.findViewById<HighlightJsView>(R.id.patch).setSource(filePatch.patch)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}