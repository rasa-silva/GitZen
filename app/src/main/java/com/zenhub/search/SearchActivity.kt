package com.zenhub.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.*
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import com.zenhub.showExceptionOnSnackbar
import com.zenhub.user.RepoListAdapter
import com.zenhub.user.RepoListType
import com.zenhub.user.UserListAdapter
import com.zenhub.user.UserListType
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class SearchActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val header by lazy { findViewById<TextView>(R.id.header) }

    private enum class SourceType {REPOS, USERS }

    private var source = SourceType.REPOS

    private val repoAdapter by lazy { RepoListAdapter(recyclerView, RepoListType.SEARCHED) }
    private val userAdapter by lazy { UserListAdapter(recyclerView, UserListType.SEARCHED) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupSearchSourceSpinner()

        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
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

    private fun setupSearchSourceSpinner() {
        val spinner = findViewById<Spinner>(R.id.search_src)
        spinner.adapter = ArrayAdapter.createFromResource(this, R.array.search_sources, R.layout.support_simple_spinner_dropdown_item)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(Application.LOGTAG, "Nothing selected")
            }

            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                source = when (pos) {
                    1 -> SourceType.USERS
                    else -> SourceType.REPOS
                }

                recyclerView.adapter = when (source) {
                    SourceType.REPOS -> repoAdapter
                    SourceType.USERS -> userAdapter
                }
            }
        }
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
            val progressBar = findViewById<FrameLayout>(R.id.progress_overlay)
            progressBar.visibility = View.VISIBLE

            when (source) {
                SourceType.REPOS -> {
                    val result = gitHubService.searchRepos(query.toString()).awaitResult()
                    when (result) {
                        is Result.Ok -> {
                            header.text = header.resources.getString(R.string.search_result_count, result.value.total_count)
                            val mappedResult = Result.Ok(result.value.items, result.response)
                            val adapter = recyclerView.adapter as RepoListAdapter
                            adapter.updateDataSet(mappedResult)
                        }
                        is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
                        is Result.Exception -> showExceptionOnSnackbar(recyclerView, result.exception)
                    }
                }
                SourceType.USERS -> {
                    val result = gitHubService.searchUsers(query.toString()).awaitResult()
                    when (result) {
                        is Result.Ok -> {
                            header.text = header.resources.getString(R.string.search_result_count, result.value.total_count)
                            val mappedResult = Result.Ok(result.value.items, result.response)
                            val adapter = recyclerView.adapter as UserListAdapter
                            adapter.updateDataSet(mappedResult)
                        }
                        is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
                        is Result.Exception -> showExceptionOnSnackbar(recyclerView, result.exception)
                    }
                }
            }

            progressBar.visibility = View.GONE
        }
    }
}
