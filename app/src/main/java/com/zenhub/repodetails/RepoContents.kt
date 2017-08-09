package com.zenhub.repodetails

import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.github.RepoContentEntry
import com.zenhub.github.gitHubService
import com.zenhub.github.showGitHubApiError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun buildContentsView(inflater: LayoutInflater, container: ViewGroup, fullRepoName: String): View {
    val view = inflater.inflate(R.layout.repo_content_contents, container, false)
    val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.contents_swiperefresh)
    val recyclerViewAdapter = ContentsRecyclerViewAdapter()
    val onContentsResponse = OnContentsResponse(recyclerViewAdapter, refreshLayout)
    refreshLayout?.setOnRefreshListener {
        Log.d(Application.LOGTAG, "Refreshing repo contents...")
        gitHubService.repoContents(onContentsResponse.etag, fullRepoName, "").enqueue(onContentsResponse)
    }

    view.findViewById<RecyclerView>(R.id.list).let {
        val layoutManager = LinearLayoutManager(it.context)
        it.layoutManager = layoutManager
        it.adapter = recyclerViewAdapter
//        it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
    }

    gitHubService.repoContents(onContentsResponse.etag, fullRepoName, "").enqueue(onContentsResponse)

    return view
}

class ContentsRecyclerViewAdapter : RecyclerView.Adapter<ContentsRecyclerViewAdapter.ViewHolder>() {

    val dataSet = mutableListOf<RepoContentEntry>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_content_contents_item, parent, false)
        return ViewHolder(view, "/")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = dataSet[position]
        holder.path = entry.path
        if (entry.type == "file") {
            val imageView = holder.itemView.findViewById<ImageView>(R.id.contentType)
            imageView.setImageResource(R.drawable.ic_insert_drive_file_white_24px)
        }

        holder.itemView.findViewById<TextView>(R.id.name).text = entry.name
        val size = if (entry.size > 1024) {
            val inKiloBytes = entry.size / 1024
            inKiloBytes.toString() + " kb"
        } else {
            entry.size.toString() + " b"
        }
        holder.itemView.findViewById<TextView>(R.id.size).text = size
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun updateDataSet(newDataSet: List<RepoContentEntry>) {
        dataSet.clear()
        dataSet.addAll(newDataSet)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View, var path: String) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                Toast.makeText(itemView.context, path, Toast.LENGTH_LONG).show()
            }
        }
    }
}

class OnContentsResponse(val adapter: ContentsRecyclerViewAdapter,
                        val parent: ViewGroup) : Callback<List<RepoContentEntry>> {

    var etag: String? = null

    override fun onFailure(call: Call<List<RepoContentEntry>>?, t: Throwable?) {
        Log.d(Application.LOGTAG, "Failed: ${t.toString()}")
    }

    override fun onResponse(call: Call<List<RepoContentEntry>>?, response: Response<List<RepoContentEntry>>) {
        Log.d(Application.LOGTAG, "contents reponse")
        when {
            response.code() == 304 -> Unit
            !response.isSuccessful -> showGitHubApiError(response.errorBody(), parent)
            else -> {
                etag = response.headers()["ETag"]
                response.body()?.let { adapter.updateDataSet(it) }
            }
        }

        val refreshLayout = parent.findViewById<SwipeRefreshLayout>(R.id.contents_swiperefresh)
        refreshLayout.isRefreshing = false
    }
}