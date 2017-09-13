package com.zenhub.gist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.auth.LoggedUser
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.Gist
import com.zenhub.showErrorOnSnackbar
import com.zenhub.showInfoOnSnackbar
import kotlinx.android.synthetic.main.activity_gist_list.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class GistListActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val adapter by lazy { GistListAdapter(this) }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gist_list_options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create -> {
                startActivityForResult(Intent(this, NewGistActivity::class.java), Companion.REQ_CREATE_GIST)
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Companion.REQ_CREATE_GIST && resultCode == Activity.RESULT_OK) {
            val gist = data?.getSerializableExtra("GIST") as Gist
            Log.d(Application.LOGTAG, "Gist with id ${gist.id} created.")
            showInfoOnSnackbar(recyclerView, "Gist created.")
            requestDataRefresh()
        } else if (requestCode == Companion.REQ_GIST_DETAILS && resultCode == Companion.RESULT_GIST_DELETED) {
            showInfoOnSnackbar(recyclerView, "Gist deleted.")
            requestDataRefresh()
        }
    }

    private fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing starred list...")
            val progressBar = findViewById<FrameLayout>(R.id.progress_overlay)
            progressBar.visibility = View.VISIBLE

            val result = gitHubService.listGists(user).awaitResult()
            when (result) {
                is Result.Ok -> {
                    val sorted = result.value.sortedByDescending { it.updated_at }
                    adapter.updateDataSet(Result.Ok(sorted, result.response))
                }
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }

            progressBar.visibility = View.GONE
            swiperefresh.isRefreshing = false
        }
    }

    companion object {
        val REQ_GIST_DETAILS = 1
        val REQ_CREATE_GIST = 0
        val RESULT_GIST_DELETED = 1
    }

}
