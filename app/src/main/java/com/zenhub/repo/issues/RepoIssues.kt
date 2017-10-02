package com.zenhub.repo.issues

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.RadioButton
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.GraphQLPagedViewAdapter
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.Issue
import com.zenhub.github.IssueOrderField
import com.zenhub.github.Result
import com.zenhub.github.fetchRepoIssues
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class IssuesFragment : Fragment() {

    private val owner by lazy { arguments.getString("REPO_NAME").substringBefore('/') }
    private val repo by lazy { arguments.getString("REPO_NAME").substringAfter('/') }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.repo_issues, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.list)
        val recyclerViewAdapter = IssuesViewAdapter(owner, repo, recyclerView)
        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.issues_swiperefresh)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.issue_list_options, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                IssuesSortDialogFragment().show(fragmentManager, "sort")
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestDataRefresh(swipeRefresh: SwipeRefreshLayout, adapter: IssuesViewAdapter) {
        launch(CommonPool) {
            Log.d(Application.LOGTAG, "Refreshing repo issues...")
            val result = fetchRepoIssues(owner, repo, IssueOrderField.UPDATED_AT)
            when (result) {
                is Result.Ok -> {
                    val issues = result.value.data.repository.issues
                    adapter.updateDataSet(issues.pageInfo, issues.nodes)
                }
                is Result.Fail -> showErrorOnSnackbar(swipeRefresh, result.error)
            }

            swipeRefresh.post { swipeRefresh.isRefreshing = false }
        }
    }


}

class IssuesViewAdapter(private val owner: String,
                        private val repo: String,
                        private val recyclerView: RecyclerView)
    : GraphQLPagedViewAdapter<Issue?>(recyclerView.context, R.layout.repo_issues_item) {

    override fun bindData(itemView: View, model: Issue?) {
        val issue = model ?: return
        itemView.findViewById<TextView>(R.id.number).text = "${issue.number}."
        itemView.findViewById<TextView>(R.id.description).text = issue.title
        itemView.findViewById<TextView>(R.id.updated_at).text = issue.updatedAt.asFuzzyDate()
        itemView.findViewById<TextView>(R.id.author).text = issue.author.login
    }

    override fun doPageRequest(endCursor: String) {
        launch(CommonPool) {
            val result = fetchRepoIssues(owner, repo, IssueOrderField.UPDATED_AT, endCursor)
            when (result) {
                is Result.Ok -> {
                    val issues = result.value.data.repository.issues
                    updateDataSet(issues.pageInfo, issues.nodes, true)
                }
                is Result.Fail -> showErrorOnSnackbar(recyclerView, result.error)
            }
        }
    }
}

enum class IssueState { OPEN, CLOSED, BOTH }

class IssuesSortDialogFragment : DialogFragment() {

    private var show = IssueState.OPEN
    private var sortBy = IssueOrderField.UPDATED_AT

//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        fragment = context as IssuesFragment
//        selected = activity.sortBy
//    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity.layoutInflater.inflate(R.layout.dialog_issues_filters, null)

        val open = view.findViewById<RadioButton>(R.id.radio_open)
        open.isChecked = true
        open.setOnClickListener { show = IssueState.OPEN }
        view.findViewById<RadioButton>(R.id.radio_closed).setOnClickListener { show = IssueState.CLOSED }
        view.findViewById<RadioButton>(R.id.radio_both).setOnClickListener { show = IssueState.BOTH }

        val updatedAt = view.findViewById<RadioButton>(R.id.radio_updatedAt)
        updatedAt.isChecked = true
        updatedAt.setOnClickListener { sortBy = IssueOrderField.UPDATED_AT }
        view.findViewById<RadioButton>(R.id.radio_createdAt).setOnClickListener { sortBy = IssueOrderField.CREATED_AT }
        view.findViewById<RadioButton>(R.id.radio_comments).setOnClickListener { sortBy = IssueOrderField.COMMENTS }

        return AlertDialog.Builder(context)
                .setTitle("Filtering & Sorting")
                .setView(view)
//                .setPositiveButton(android.R.string.ok,
//                        { _, _ -> activity.switchOrdering(selected) })
                .setNegativeButton(android.R.string.cancel, { _, _ -> })
                .create()
    }

}
