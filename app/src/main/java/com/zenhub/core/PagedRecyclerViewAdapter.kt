package com.zenhub.core

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zenhub.R
import okhttp3.Response
import ru.gildor.coroutines.retrofit.Result


abstract class PagedRecyclerViewAdapter<in T>(ctx: Context,
                                              @LayoutRes private val itemLayout: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater by lazy { LayoutInflater.from(ctx) }
    private val dataSet = mutableListOf<ModelHolder<T>>()
    private val TYPE_VIEW = 0
    private val TYPE_LOAD = 1
    private var isLoading = false
    private var isMoreDataAvailable = false
    private var totalPages = 0
    private var currentPage = 1
    private var templateUrl = ""
    private lateinit var view: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        view = recyclerView
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position >= itemCount - 1 && isMoreDataAvailable && !isLoading) {
            if (isMoreDataAvailable) {
                currentPage++
                val url = templateUrl.replaceAfterLast("?page=", currentPage.toString())
                loadMore(url)
                if (currentPage == totalPages) isMoreDataAvailable = false
            }
        }

        if (getItemViewType(position) == TYPE_VIEW) {
            bindData(holder.itemView, dataSet[position].model)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_LOAD) {
            Holder(inflater.inflate(R.layout.list_item_load, parent, false))
        } else {
            Holder(inflater.inflate(itemLayout, parent, false))
        }
    }

    override fun getItemViewType(position: Int) = dataSet[position].type

    override fun getItemCount() = dataSet.size

    abstract fun bindData(itemView: View, model: T?)

    abstract fun doPageRequest(url: String)

    private fun loadMore(url: String) {
        isLoading = true
        view.post {
            dataSet.add(ModelHolder(TYPE_LOAD, null))
            notifyItemInserted(dataSet.size - 1)
            doPageRequest(url)
        }
    }

    /**
     * This should be called for first page results.
     */
    fun updateDataSet(newDataSet: Result.Ok<List<T>>) {
        val (lastPage, url) = extractPaginationInfo(newDataSet.response)

        if (lastPage > 1) isMoreDataAvailable = true
        totalPages = lastPage
        templateUrl = url

        dataSet.clear()
        dataSet.addAll(newDataSet.value.map { ModelHolder(TYPE_VIEW, it) })
        notifyDataSetChanged()
        isLoading = false
    }

    /**
     * This should be called for page > 1 results.
     */
    fun appendDataSet(newDataSet: List<T>) {
        dataSet.removeAt(dataSet.size - 1)
        dataSet.addAll(newDataSet.map { ModelHolder(TYPE_VIEW, it) })
        notifyDataSetChanged()
        isLoading = false
    }

    private fun extractPaginationInfo(response: Response): Pair<Int, String> {
        val linkHeader = response.header("Link") ?: return 1 to ""
        val nextAndLastUrls = linkHeader.split(';', ',')
        val lastUrl = nextAndLastUrls[2].trim(' ', '<', '>')
        val lastPage = lastUrl.substringAfterLast("page=").toInt()
        return lastPage to lastUrl
    }

    internal class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)

    internal class ModelHolder<out T>(val type: Int, val model: T?)
}

