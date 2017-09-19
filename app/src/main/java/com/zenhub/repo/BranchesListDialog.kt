package com.zenhub.repo

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.PagedRecyclerViewAdapter
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.Branch
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult


class BranchesListDialog(ctx: Context, private val fullRepoName: String,
                         private val onSelection: (String) -> Unit) {

    private val items = RecyclerView(ctx)
    private val adapter = BranchesListAdapter(ctx, items)
    private val dialog = AlertDialog.Builder(ctx).setView(items).create()

    init {
        items.layoutManager = LinearLayoutManager(ctx)
        items.adapter = adapter
        items.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {

            private val gd = GestureDetector(Application.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent) = true
            })

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (gd.onTouchEvent(e)) {
                    val view = items.findChildViewUnder(e.x, e.y)
                    dialog.dismiss()
                    val text = (view as TextView).text.toString()
                    onSelection(text)
                }

                return false
            }
        })
    }

    fun show() {
        launch(UI) {
            dialog.show()
            Log.d(Application.LOGTAG, "Refreshing repo branches...")
            val result = gitHubService.branches(fullRepoName).awaitResult()
            when (result) {
                is Result.Ok -> adapter.updateDataSet(result)
                is Result.Error -> showErrorOnSnackbar(items, result.response.message())
                is Result.Exception -> showErrorOnSnackbar(items, result.exception.localizedMessage)
            }
        }
    }
}

class BranchesListAdapter(ctx: Context, private val recyclerView: RecyclerView) :
        PagedRecyclerViewAdapter<Branch?>(ctx, R.layout.support_simple_spinner_dropdown_item) {

    override fun bindData(itemView: View, model: Branch?) {
        val branch = model ?: return
        val textView = itemView.findViewById<TextView>(android.R.id.text1)
        textView.text = branch.name
    }

    override fun doPageRequest(url: String) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Paging branches list with $url...")
            val result = gitHubService.branchesPaginate(url).awaitResult()
            when (result) {
                is Result.Ok -> appendDataSet(result.value)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}