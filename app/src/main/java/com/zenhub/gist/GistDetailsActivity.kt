package com.zenhub.gist

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
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import com.zenhub.showInfoOnSnackbar
import kotlinx.android.synthetic.main.activity_gist_details.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResponse
import ru.gildor.coroutines.retrofit.awaitResult

class GistDetailsActivity : AppCompatActivity() {

    private val url by lazy { intent.getStringExtra("URL") }
    private val id by lazy { url.substringAfterLast('/') }
    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gist_details)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = GistFilesAdapter(this)
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        swiperefresh.setOnRefreshListener { requestDataRefresh() }

        requestDataRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gist_details_options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                deleteGist()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteGist() {
        launch(UI) {
            val response = gitHubService.deleteGist(id).awaitResponse()
            when {
                response.isSuccessful -> showInfoOnSnackbar(recyclerView, "Gist deleted.")
                else -> showErrorOnSnackbar(recyclerView, response.message())
            }
        }
    }

    private fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Fetching gist $url...")
            val progressBar = findViewById<FrameLayout>(R.id.progress_overlay)
            progressBar.visibility = View.VISIBLE
            val result = gitHubService.gist(url).awaitResult()
            when (result) {
                is Result.Ok -> {
                    val gist = result.value
                    description.text = gist.description
                    val updated = resources.getString(R.string.last_updated, gist.updated_at.asFuzzyDate())
                    updated_at.text = updated
                    (recyclerView.adapter as GistFilesAdapter).update(gist.files.values)
                }
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }

            progressBar.visibility = View.GONE
            swiperefresh.isRefreshing = false
        }

    }
}
