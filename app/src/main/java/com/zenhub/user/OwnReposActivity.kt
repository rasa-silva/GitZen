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

class OwnReposActivity : RepoListActivity() {

    override fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing list...")
            val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
            val result = gitHubService.listRepos().awaitResult()
            when (result) {
                is Result.Ok -> adapter.updateDataSet(result.value)
                is Result.Error -> showErrorOnSnackbar(refreshLayout, result.response.message())
                is Result.Exception -> showErrorOnSnackbar(refreshLayout, result.exception.localizedMessage)
            }
        }
    }
}