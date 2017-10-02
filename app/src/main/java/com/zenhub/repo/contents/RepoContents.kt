package com.zenhub.repo.contents

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.asDigitalUnit
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.RepoContentEntry
import com.zenhub.showErrorOnSnackbar
import com.zenhub.showExceptionOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class ContentsFragment : Fragment() {

    private val fullRepoName by lazy { arguments.getString("REPO_NAME") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.repo_content_contents, container, false)
        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.contents_swiperefresh)
        val recyclerViewAdapter = ContentsRecyclerViewAdapter(refreshLayout)
        refreshLayout?.setOnRefreshListener { requestData("", refreshLayout, recyclerViewAdapter) }

        view.findViewById<RecyclerView>(R.id.list).let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = recyclerViewAdapter
        }

        val currentPath = view.findViewById<TextView>(R.id.current_path)
        view.findViewById<ImageView>(R.id.back).setOnClickListener {
            val currentPathText = currentPath.text
            if (currentPathText == "/") return@setOnClickListener

            val newPath = if (currentPathText.count { it == '/' } == 0) ""
            else currentPathText.substring(0, currentPathText.lastIndexOf('/'))

            Log.d(Application.LOGTAG, "Going back to $newPath...")
            requestData(newPath, refreshLayout, recyclerViewAdapter)
        }

        requestData("", refreshLayout, recyclerViewAdapter)

        return view
    }

    private fun requestData(path: String, parentView: View, adapter: ContentsRecyclerViewAdapter) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing repo contents...")
            //TODO The contents tab also needs to switch between branches...
            val response = gitHubService.repoContents(fullRepoName, path).awaitResult()
            when (response) {
                is Result.Ok -> {
                    parentView.findViewById<TextView>(R.id.current_path).text = path
                    adapter.updateDataSet(response.value)
                }
                is Result.Error -> showErrorOnSnackbar(parentView, response.response.message())
                is Result.Exception -> showExceptionOnSnackbar(parentView, response.exception)
            }

            val refreshLayout = parentView.findViewById<SwipeRefreshLayout>(R.id.contents_swiperefresh)
            refreshLayout.isRefreshing = false
        }
    }

    inner class ContentsRecyclerViewAdapter(private val refreshLayout: SwipeRefreshLayout) : RecyclerView.Adapter<ContentsRecyclerViewAdapter.ViewHolder>() {

        private val dataSet = mutableListOf<RepoContentEntry>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_content_contents_item, parent, false)
            val entry = RepoContentEntry("", "/", 0, "dir", "")
            return ViewHolder(view, this, refreshLayout, entry)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = dataSet[position]
            holder.entry = entry
            if (entry.type == "file") {
                val imageView = holder.itemView.findViewById<ImageView>(R.id.contentType)
                imageView.setImageResource(R.drawable.ic_insert_drive_file_white_24px)
            }

            holder.itemView.findViewById<TextView>(R.id.name).text = entry.name
            holder.itemView.findViewById<TextView>(R.id.size).text = entry.size.asDigitalUnit()
        }

        override fun getItemCount(): Int {
            return dataSet.size
        }

        fun updateDataSet(newDataSet: List<RepoContentEntry>) {
            dataSet.clear()
            dataSet.addAll(newDataSet)
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View,
                               private val adapter: ContentsRecyclerViewAdapter,
                               private val rootView: View,
                               var entry: RepoContentEntry) : RecyclerView.ViewHolder(itemView) {
            init {
                itemView.setOnClickListener {
                    if (entry.type == "file") {
                        val intent = Intent(itemView.context, FileContentsActivity::class.java)
                        intent.putExtra("FILE_URL", entry.download_url)
                        ContextCompat.startActivity(itemView.context, intent, null)
                    } else {
                        requestData(entry.path, rootView, adapter)
                    }
                }
            }
        }
    }
}