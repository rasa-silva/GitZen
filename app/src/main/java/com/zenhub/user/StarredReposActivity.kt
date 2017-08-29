package com.zenhub.user

import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class StarredReposActivity : RepoListActivity() {

    override fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing list...")
            val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
            doFirstRequest()
            refreshLayout.isRefreshing = false
        }
    }

    suspend private fun doFirstRequest() {
        val result = gitHubService.listStarred().awaitResult()
        when (result) {
            is Result.Ok -> {
                adapter.updateDataSet(result.value)
                attachPaginationListener(recyclerView, result.response, this::doPaginationRequest)
            }
            is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
            is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
        }
    }

    private fun doPaginationRequest(url: String) {
        launch(UI) {
            val result = gitHubService.listStarredPaginate(url).awaitResult()
            when (result) {
                is Result.Ok -> adapter.appendData(result.value)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}