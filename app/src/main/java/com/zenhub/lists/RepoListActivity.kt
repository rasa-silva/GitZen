package com.zenhub.lists

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.BaseActivity
import com.zenhub.R
import com.zenhub.github.Repository
import com.zenhub.github.dateFormat
import com.zenhub.github.gitHubService
import com.zenhub.github.languageColors
import com.zenhub.repo.RepoActivity
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.Response
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class StarredReposActivity : RepoListActivity() {

    override fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing list...")
            val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
            val result = gitHubService.listStarred().awaitResult()
            when (result) {
                is Result.Ok -> {
                    adapter.updateDataSet(result.value)
                    paginate(refreshLayout, result.response)
                }
                is Result.Error -> showErrorOnSnackbar(refreshLayout, result.response.message())
                is Result.Exception -> showErrorOnSnackbar(refreshLayout, result.exception.localizedMessage)
            }

            refreshLayout.isRefreshing = false
        }
    }

    private suspend fun paginate(rootView: View, response: Response) {
        val linkHeader = response.header("Link") ?: return
        val nextAndLastUrls = linkHeader.split(';', ',')
        val lastUrl = nextAndLastUrls[2].trim(' ', '<', '>')

        val lastPage = lastUrl.substringAfterLast("?page=").toInt()
        var nextPage = 2

        while (nextPage <= lastPage) {
            val url = lastUrl.replaceAfterLast("?page=", nextPage.toString())
            Log.d(Application.LOGTAG, "Requesting pagination $url")
            val result = gitHubService.listStarredPaginate(url).awaitResult()
            when (result) {
                is Result.Ok -> adapter.appendData(result.value)
                is Result.Error -> showErrorOnSnackbar(rootView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(rootView, result.exception.localizedMessage)
            }

            nextPage++
        }
    }
}

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

abstract class RepoListActivity : BaseActivity() {

    val adapter = RepoListRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_list_activity)
        super.onCreateDrawer()

        findViewById<RecyclerView>(R.id.list).let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        requestDataRefresh()
    }

    abstract override fun requestDataRefresh()
}

class RepoListRecyclerViewAdapter : RecyclerView.Adapter<RepoListRecyclerViewAdapter.ViewHolder>() {

    private val dataSet = mutableListOf<Repository>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.repo_list_item, parent, false)
        return ViewHolder(parent.context, view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val starredRepo = dataSet[position]
        val date = dateFormat.parse(starredRepo.pushed_at)
        val fuzzy_date = DateUtils.getRelativeTimeSpanString(date.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
        holder.itemView.findViewById<TextView>(R.id.repo_full_name).text = starredRepo.full_name
        holder.itemView.findViewById<TextView>(R.id.repo_description).text = starredRepo.description
        holder.itemView.findViewById<TextView>(R.id.repo_pushed_time).text = fuzzy_date
        val stars = holder.itemView.resources.getString(R.string.repo_stars, starredRepo.stargazers_count)
        holder.itemView.findViewById<TextView>(R.id.repo_stars).text = stars
        val languageTextView = holder.itemView.findViewById<TextView>(R.id.repo_language)
        languageTextView.text = starredRepo.language
        if (starredRepo.language == null) {
            languageTextView.background = ColorDrawable(Color.TRANSPARENT)
        } else {
            val color = languageColors[starredRepo.language]?.color
            color?.let { languageTextView.background = ColorDrawable(Color.parseColor(it)) }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun updateDataSet(newDataSet: List<Repository>) {
        dataSet.clear()
        dataSet.addAll(newDataSet)
        notifyDataSetChanged()
    }

    fun appendData(newDataSet: List<Repository>) {
        dataSet.addAll(newDataSet)
        notifyDataSetChanged()
    }

    class ViewHolder(ctx: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val textView = itemView.findViewById<TextView>(R.id.repo_full_name)
                val intent = Intent(ctx, RepoActivity::class.java)
                intent.putExtra("REPO_FULL_NAME", textView.text.toString())
                ContextCompat.startActivity(ctx, intent, null)
            }
        }
    }
}
