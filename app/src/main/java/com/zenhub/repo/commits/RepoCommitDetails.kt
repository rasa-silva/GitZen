package com.zenhub.repo.commits

import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pddstudio.highlightjs.HighlightJsView
import com.pddstudio.highlightjs.models.Language
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.config.getHighlightJsTheme
import com.zenhub.github.CommitFile
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import com.zenhub.showExceptionOnSnackbar
import kotlinx.android.synthetic.main.progress_bar_overlay.*
import kotlinx.android.synthetic.main.repo_commit_activity.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class RepoCommitDetails : AppCompatActivity() {

    private val repoName by lazy { intent.getStringExtra("REPO_FULL_NAME") }
    private val commitSha by lazy { intent.getStringExtra("COMMIT_SHA") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_commit_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = repoName

        findViewById<RecyclerView>(R.id.files).let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = CommitFilesRecyclerViewAdapter()
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        swiperefresh.setOnRefreshListener { requestDataRefresh() }
        requestDataRefresh()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpTo(this, parentActivityIntent.putExtra("REPO_FULL_NAME", repoName))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing commit details...")
            progress_overlay.visibility = View.VISIBLE

            val result = gitHubService.commit(repoName, commitSha).awaitResult()
            when (result) {
                is Result.Ok -> {
                    val response = result.value
                    val firstAndOtherLines = response.commit.message.split("\n\n")
                    val styledCommitMsg = if (firstAndOtherLines.size == 1) response.commit.message
                    else {
                        SpannableStringBuilder()
                                .append(firstAndOtherLines[0])
                                .append('\n')
                                .append(firstAndOtherLines[1], RelativeSizeSpan(0.75f), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    swiperefresh.findViewById<TextView>(R.id.commit_message).text = styledCommitMsg

                    val commit_details_stats = swiperefresh.findViewById<TextView>(R.id.commit_details_stats)
                    val statsText = commit_details_stats.resources.getString(R.string.commit_details_stats, response.commit.committer.name, response.files.size)
                    commit_details_stats.text = statsText

                    val adapter = swiperefresh.findViewById<RecyclerView>(R.id.files).adapter as CommitFilesRecyclerViewAdapter
                    adapter.updateDataSet(response.files)
                }
                is Result.Error -> showErrorOnSnackbar(swiperefresh, result.response.message())
                is Result.Exception -> showExceptionOnSnackbar(swiperefresh, result.exception)
            }

            progress_overlay.visibility = View.GONE
            swiperefresh.isRefreshing = false
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
            theme = getHighlightJsTheme()
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