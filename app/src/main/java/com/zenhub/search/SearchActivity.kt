package com.zenhub.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.util.Log
import android.view.Menu
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
import com.zenhub.showExceptionOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult


class SearchActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val header by lazy { findViewById<TextView>(R.id.header) }
    private val adapter by lazy { SearchListAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        header.text = header.resources.getString(R.string.search_result_count, 0)

        handleIntent()

    }

    override fun onNewIntent(intent: Intent) {
        this.intent = intent
        handleIntent()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_options, menu)

        val searchService = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.setSearchableInfo(searchService.getSearchableInfo(componentName))
        searchView.setIconifiedByDefault(false)
        searchView.isSubmitButtonEnabled = true
        searchView.requestFocus()
        return true
    }

    private fun handleIntent() {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            val suggestions = SearchRecentSuggestions(this, RecentSearchesProvider.AUTHORITY, RecentSearchesProvider.MODE)
            suggestions.saveRecentQuery(query, null)
            doSearch(query)
        }
    }

    private fun doSearch(query: CharSequence) {
        Log.d(Application.LOGTAG, "Searching for $query")
        launch(UI) {
            val result = gitHubService.searchRepos(query.toString()).awaitResult()
            when (result) {
                is Result.Ok -> {

                    header.text = header.resources.getString(R.string.search_result_count, result.value.total_count)
                    val mappedResult = Result.Ok(result.value.items, result.response)
                    adapter.updateDataSet(mappedResult)
                }
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
                is Result.Exception -> showExceptionOnSnackbar(recyclerView, result.exception)
            }
        }
    }

}

class SearchListAdapter(private val activity: SearchActivity) : PagedRecyclerViewAdapter<Repository?>(activity, R.layout.repo_list_item) {

    private val recyclerView = activity.findViewById<RecyclerView>(R.id.list)

    override fun bindData(itemView: View, model: Repository?) {
        val repo = model ?: return

        val repoNameView = itemView.findViewById<TextView>(R.id.repo_full_name)
        repoNameView.text = repo.full_name
        itemView.findViewById<TextView>(R.id.repo_description).text = repo.description
        itemView.findViewById<TextView>(R.id.repo_pushed_time).text = repo.pushed_at.asFuzzyDate()
        val stars = itemView.resources.getString(R.string.repo_stars, repo.stargazers_count)
        itemView.findViewById<TextView>(R.id.repo_stars).text = stars
        val languageTextView = itemView.findViewById<TextView>(R.id.repo_language)
        languageTextView.text = repo.language
        languageTextView.background = getLanguageColor(repo.language)

        itemView.setOnClickListener {
            val intent = Intent(activity, RepoActivity::class.java)
            intent.putExtra("REPO_FULL_NAME", repoNameView.text.toString())
            ContextCompat.startActivity(activity, intent, null)
        }
    }

    override fun doPageRequest(url: String) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Paging search list with $url...")
            val result = gitHubService.searchReposPaginate(url).awaitResult()
            when (result) {
                is Result.Ok -> appendDataSet(result.value.items)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}
