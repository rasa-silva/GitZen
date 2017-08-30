package com.zenhub.user

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
import android.view.View
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.BaseActivity
import com.zenhub.core.PagedRecyclerViewAdapter
import com.zenhub.github.Repository
import com.zenhub.github.dateFormat
import com.zenhub.github.languageColors
import com.zenhub.repo.RepoActivity
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.Call
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

abstract class RepoListActivity : BaseActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val adapter by lazy { RepoListAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_list_activity)
        super.onCreateDrawer()

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        requestDataRefresh()
    }

    override fun requestDataRefresh() {
        launch(UI) {
            Log.d(Application.LOGTAG, "Refreshing starred list...")
            val result = requestInitial().awaitResult()
            when (result) {
                is Result.Ok -> adapter.updateDataSet(result)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
            findViewById<SwipeRefreshLayout>(R.id.swiperefresh).isRefreshing = false
        }
    }

    abstract fun requestInitial(): Call<List<Repository>>
    abstract fun requestPage(url: String) : Call<List<Repository>>
}

class RepoListAdapter(private val activity: RepoListActivity) : PagedRecyclerViewAdapter<Repository?>(activity, R.layout.repo_list_item) {
    override fun bindData(itemView: View, model: Repository?) {
        val starredRepo = model ?: return

        val date = dateFormat.parse(starredRepo.pushed_at)
        val fuzzy_date = DateUtils.getRelativeTimeSpanString(date.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
        val repoNameView = itemView.findViewById<TextView>(R.id.repo_full_name)
        repoNameView.text = starredRepo.full_name
        itemView.findViewById<TextView>(R.id.repo_description).text = starredRepo.description
        itemView.findViewById<TextView>(R.id.repo_pushed_time).text = fuzzy_date
        val stars = itemView.resources.getString(R.string.repo_stars, starredRepo.stargazers_count)
        itemView.findViewById<TextView>(R.id.repo_stars).text = stars
        val languageTextView = itemView.findViewById<TextView>(R.id.repo_language)
        languageTextView.text = starredRepo.language
        if (starredRepo.language == null) {
            languageTextView.background = ColorDrawable(Color.TRANSPARENT)
        } else {
            val color = languageColors[starredRepo.language]?.color
            color?.let { languageTextView.background = ColorDrawable(Color.parseColor(it)) }
        }

        itemView.setOnClickListener {
            val intent = Intent(activity, RepoActivity::class.java)
            intent.putExtra("REPO_FULL_NAME", repoNameView.text.toString())
            ContextCompat.startActivity(activity, intent, null)
        }
    }

    override fun doPageRequest(url: String) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Paging own repo list with $url...")
            val result = activity.requestPage(url).awaitResult()
            val recyclerView = activity.findViewById<RecyclerView>(R.id.list)
            when (result) {
                is Result.Ok -> appendDataSet(result.value)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}