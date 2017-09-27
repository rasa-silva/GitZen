package com.zenhub.core

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zenhub.R
import com.zenhub.github.PageInfo

abstract class GraphQLPagedViewAdapter<in T>(ctx: Context,
                                             @LayoutRes private val itemLayout: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater by lazy { LayoutInflater.from(ctx) }
    private val dataSet = mutableListOf<ModelHolder<T>>()
    private val TYPE_VIEW = 0
    private val TYPE_LOAD = 1
    private var isLoading = false
    private var isMoreDataAvailable = false
    private var endCursor = ""
    private lateinit var view: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        view = recyclerView
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position >= itemCount - 1 && !isLoading) {
            if (isMoreDataAvailable) loadMore()
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

    abstract fun doPageRequest(endCursor: String)

    private fun loadMore() {
        isLoading = true
        view.post {
            dataSet.add(ModelHolder(TYPE_LOAD, null))
            notifyItemInserted(dataSet.size - 1)
            doPageRequest(endCursor)
        }
    }

    fun updateDataSet(pageInfo: PageInfo, nodes: List<T>, append: Boolean = false) {
        isMoreDataAvailable = pageInfo.hasNextPage
        endCursor = pageInfo.endCursor
        val holders = nodes.map { ModelHolder(TYPE_VIEW, it) }

        view.post {
            if (append) dataSet.removeAt(dataSet.size - 1) else dataSet.clear()
            dataSet.addAll(holders)
            notifyDataSetChanged()
            isLoading = false
        }
    }

    internal class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)

    internal class ModelHolder<out T>(val type: Int, val model: T?)
}