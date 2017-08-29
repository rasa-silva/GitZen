package com.zenhub.user

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import com.zenhub.Application
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class PaginationScrollListener(private val layoutManager: LinearLayoutManager,
                               private val totalPages: Int,
                               private var templateUrl: String,
                               private val pageRequester: (String) -> Unit) : RecyclerView.OnScrollListener() {

    private var currentPage = 1
//    private var isLoading = false

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        if (/*!isLoading && */isNotLastPage()) {
            if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0) {
                loadMoreItems()
            }
        }
    }

    private fun loadMoreItems() {
//        isLoading = true
        currentPage++
        launch(UI) {
            val url = templateUrl.replaceAfterLast("?page=", currentPage.toString())
            Log.d(Application.LOGTAG, "Requesting pagination $url")
            pageRequester(url)
//            isLoading = false
        }
    }

    private fun isNotLastPage() = currentPage != totalPages
}
