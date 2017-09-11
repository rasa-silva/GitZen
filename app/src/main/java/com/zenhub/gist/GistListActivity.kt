package com.zenhub.gist

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.auth.LoggedUser
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import kotlinx.android.synthetic.main.repo_list_activity.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class GistListActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val adapter by lazy { GistListAdapter(recyclerView) }
    private val user by lazy { intent.getStringExtra("USER") ?: LoggedUser.account?.name.orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gist_list)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        swiperefresh.setOnRefreshListener { requestDataRefresh() }

        requestDataRefresh()
    }

    private fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing starred list...")
            val result = gitHubService.listGists(user).awaitResult()
            when (result) {
                is Result.Ok -> adapter.updateDataSet(result)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
            findViewById<SwipeRefreshLayout>(R.id.swiperefresh).isRefreshing = false
        }
    }
}
