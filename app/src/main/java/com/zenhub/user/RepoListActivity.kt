package com.zenhub.user

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.PagedRecyclerViewAdapter
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.Repository
import com.zenhub.github.getLanguageColor
import com.zenhub.github.gitHubService
import com.zenhub.repo.RepoActivity
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.Call
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

enum class REPO_LIST_TYPE {OWN, STARRED }

class RepoListActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val adapter by lazy { RepoListAdapter(this) }
    private val listType by lazy { intent.getSerializableExtra("LIST_TYPE") as REPO_LIST_TYPE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_list_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = if (listType == REPO_LIST_TYPE.OWN)
            resources.getString(R.string.title_activity_own_repos)
        else
            resources.getString(R.string.title_activity_starred_repos)

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        requestDataRefresh()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun requestDataRefresh() {
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

    private fun requestInitial(): Call<List<Repository>> {
        return if (listType == REPO_LIST_TYPE.OWN) gitHubService.listRepos()
        else gitHubService.listStarred()
    }

    fun requestPage(url: String): Call<List<Repository>> {
        return if (listType == REPO_LIST_TYPE.OWN) gitHubService.listReposPaginate(url)
        else gitHubService.listStarredPaginate(url)

    }
}

class RepoListAdapter(private val activity: RepoListActivity) : PagedRecyclerViewAdapter<Repository?>(activity, R.layout.repo_list_item) {
    override fun bindData(itemView: View, model: Repository?) {
        val starredRepo = model ?: return

        val repoNameView = itemView.findViewById<TextView>(R.id.repo_full_name)
        repoNameView.text = starredRepo.full_name
        itemView.findViewById<TextView>(R.id.repo_description).text = starredRepo.description
        itemView.findViewById<TextView>(R.id.repo_pushed_time).text = starredRepo.pushed_at.asFuzzyDate()
        val stars = itemView.resources.getString(R.string.repo_stars, starredRepo.stargazers_count)
        itemView.findViewById<TextView>(R.id.repo_stars).text = stars
        val languageTextView = itemView.findViewById<TextView>(R.id.repo_language)
        languageTextView.text = starredRepo.language
        languageTextView.background = getLanguageColor(starredRepo.language)

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