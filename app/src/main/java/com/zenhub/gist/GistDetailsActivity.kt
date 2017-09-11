package com.zenhub.gist

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import kotlinx.android.synthetic.main.activity_gist_details.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class GistDetailsActivity : AppCompatActivity() {

    private val url by lazy { intent.getStringExtra("URL") }
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

    private fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Fetching gist $url...")
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
        }

    }
}
