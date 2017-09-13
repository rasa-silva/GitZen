package com.zenhub.user

import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.auth.LoggedUser
import com.zenhub.github.GitHubService
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import kotlinx.android.synthetic.main.repo_list_activity.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

enum class RepoListType {OWN, STARRED, SEARCHED }

class RepoListActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val listType by lazy { intent.getSerializableExtra("LIST_TYPE") as RepoListType }
    private val adapter by lazy { RepoListAdapter(recyclerView, listType) }
    private val user by lazy { intent.getStringExtra("USER") ?: LoggedUser.account?.name.orEmpty() }
    var sortBy = GitHubService.RepoListSorting.pushed
    var starredSortBy = GitHubService.StarredReposListSorting.updated

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_list_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = if (listType == RepoListType.OWN)
            resources.getString(R.string.title_activity_own_repos)
        else
            resources.getString(R.string.title_activity_starred_repos)

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        swiperefresh.setOnRefreshListener { requestDataRefresh() }

        requestDataRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        when (listType) {
            RepoListType.OWN, RepoListType.STARRED -> menuInflater.inflate(R.menu.repo_list_options, menu)
            RepoListType.SEARCHED -> {}
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                val fragment = if (listType == RepoListType.OWN) SortByDialogFragment() else StarredSortByDialogFragment()
                    fragment.show(fragmentManager, "sortBy")
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun switchOrdering(ordering: GitHubService.RepoListSorting) {
        Log.d(Application.LOGTAG, "Switched ordering to $ordering")
        sortBy = ordering
        requestDataRefresh()
    }

    fun switchOrdering(ordering: GitHubService.StarredReposListSorting) {
        Log.d(Application.LOGTAG, "Switched ordering to $ordering")
        starredSortBy = ordering
        requestDataRefresh()
    }

    private fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing starred list...")
            val result = if (listType == RepoListType.OWN) gitHubService.listRepos(user, sortBy).awaitResult()
            else gitHubService.listStarred(user, starredSortBy).awaitResult()
            when (result) {
                is Result.Ok -> adapter.updateDataSet(result)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
            findViewById<SwipeRefreshLayout>(R.id.swiperefresh).isRefreshing = false
        }
    }
}

class SortByDialogFragment : DialogFragment() {

    private val values = GitHubService.RepoListSorting.values().map { it.desc }.toTypedArray()
    private lateinit var activity: RepoListActivity
    private lateinit var selected: GitHubService.RepoListSorting

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as RepoListActivity
        selected = activity.sortBy
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.repo_list_sort_dialog_title))
                .setSingleChoiceItems(values, selected.ordinal,
                        { _, selectedIndex -> selected = GitHubService.RepoListSorting.values()[selectedIndex] })
                .setPositiveButton(android.R.string.ok,
                        { _, _ -> activity.switchOrdering(selected) })
                .setNegativeButton(android.R.string.cancel, {_, _ -> })
                .create()
    }
}

class StarredSortByDialogFragment : DialogFragment() {

    private val values = GitHubService.StarredReposListSorting.values().map { it.desc }.toTypedArray()
    private lateinit var activity: RepoListActivity
    private lateinit var selected: GitHubService.StarredReposListSorting

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as RepoListActivity
        selected = activity.starredSortBy
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity)
                .setTitle(getString(R.string.repo_list_sort_dialog_title))
                .setSingleChoiceItems(values, selected.ordinal,
                        { _, selectedIndex -> selected = GitHubService.StarredReposListSorting.values()[selectedIndex] })
                .setPositiveButton(android.R.string.ok,
                        { _, _ -> activity.switchOrdering(selected) })
                .setNegativeButton(android.R.string.cancel, {_, _ -> })
                .create()
    }
}