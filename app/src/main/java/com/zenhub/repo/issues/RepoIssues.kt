package com.zenhub.repo.issues

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.GraphQLPagedViewAdapter
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.Fail
import com.zenhub.github.Issue
import com.zenhub.github.Ok
import com.zenhub.github.fetchRepoIssues
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class IssuesFragment : Fragment() {

    private val owner by lazy { arguments.getString("REPO_NAME").substringBefore('/') }
    private val repo by lazy { arguments.getString("REPO_NAME").substringAfter('/') }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.repo_issues, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.list)
        val recyclerViewAdapter = IssuesViewAdapter(owner, repo, recyclerView)
        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.commits_swiperefresh)
        refreshLayout?.setOnRefreshListener { requestDataRefresh(refreshLayout, recyclerViewAdapter) }

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = recyclerViewAdapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        requestDataRefresh(refreshLayout, recyclerViewAdapter)

        return view

    }

    private fun requestDataRefresh(swipeRefresh: SwipeRefreshLayout, adapter: IssuesViewAdapter) {
        launch(CommonPool) {
            Log.d(Application.LOGTAG, "Refreshing repo issues...")
            val result = fetchRepoIssues(owner, repo)
            when (result) {
                is Ok -> {
                    val issues = result.value.data.repository.issues
                    adapter.updateDataSet(issues.pageInfo, issues.nodes)
                }
                is Fail -> showErrorOnSnackbar(swipeRefresh, result.error)
            }

            swipeRefresh.post { swipeRefresh.isRefreshing = false }
        }
    }


    class IssuesViewAdapter(private val owner: String, private val repo: String, private val recyclerView: RecyclerView)
        : GraphQLPagedViewAdapter<Issue?>(recyclerView.context, R.layout.repo_issues_item) {

        override fun bindData(itemView: View, model: Issue?) {
            val issue = model ?: return
            itemView.findViewById<TextView>(R.id.description).text = issue.title
            itemView.findViewById<TextView>(R.id.updated_at).text = issue.updatedAt.asFuzzyDate()
        }

        override fun doPageRequest(endCursor: String) {
            launch(CommonPool) {
                val result = fetchRepoIssues(owner, repo, endCursor)
                when (result) {
                    is Ok -> {
                        val issues = result.value.data.repository.issues
                        updateDataSet(issues.pageInfo, issues.nodes, true)
                    }
                    is Fail -> showErrorOnSnackbar(recyclerView, result.error)
                }
            }
        }
    }
}