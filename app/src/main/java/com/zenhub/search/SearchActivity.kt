package com.zenhub.search

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.*
import com.zenhub.*
import com.zenhub.core.PagedRecyclerViewAdapter
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.*
import com.zenhub.repo.RepoActivity
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult


class SearchActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.list) }
    private val header by lazy { findViewById<TextView>(R.id.header) }
//    private val adapter by lazy { SearchListAdapter(this) }

    private enum class SourceType {REPOS, USERS }

    private var source = SourceType.REPOS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupSearchSourceSpinner()

        setListAdapter()
        recyclerView.let {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.addItemDecoration(DividerItemDecoration(it.context, layoutManager.orientation))
        }

        header.text = header.resources.getString(R.string.search_result_count, 0)

        handleIntent()

    }

    private fun setListAdapter() {
        recyclerView.adapter = when (source) {
            SourceType.REPOS -> RepoSearchAdapter(this)
            SourceType.USERS -> UserSearchAdapter(this)
        }
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

                setListAdapter()
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

            val call = when (source) {
                SourceType.REPOS -> gitHubService.searchRepos(query.toString())
                SourceType.USERS -> gitHubService.searchUsers(query.toString())
            }

            val result = call.awaitResult()

            when (result) {
                is Result.Ok -> {
                    when (source) {
                        SearchActivity.SourceType.REPOS -> {
                            val repos = result.value as RepositorySearch
                            header.text = header.resources.getString(R.string.search_result_count, repos.total_count)
                            val mappedResult = Result.Ok(repos.items, result.response)
                            val adapter = recyclerView.adapter as RepoSearchAdapter
                            adapter.updateDataSet(mappedResult)
                        }
                        SearchActivity.SourceType.USERS -> {
                            val users = result.value as UserSearch
                            header.text = header.resources.getString(R.string.search_result_count, users.total_count)
                            val mappedResult = Result.Ok(users.items, result.response)
                            val adapter = recyclerView.adapter as UserSearchAdapter
                            adapter.updateDataSet(mappedResult)
                        }
                    }

                }
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.response.message())
                is Result.Exception -> showExceptionOnSnackbar(recyclerView, result.exception)
            }

            progressBar.visibility = View.GONE
        }
    }

}

class RepoSearchAdapter(private val activity: SearchActivity) : PagedRecyclerViewAdapter<Repository?>(activity, R.layout.repo_list_item) {

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

class UserSearchAdapter(private val activity: SearchActivity) : PagedRecyclerViewAdapter<User?>(activity, R.layout.user_item) {

    private val recyclerView = activity.findViewById<RecyclerView>(R.id.list)

    override fun bindData(itemView: View, model: User?) {
        val user = model ?: return

        itemView.findViewById<TextView>(R.id.login).text = user.login
        val avatar = itemView.findViewById<ImageView>(R.id.avatar)
        Application.picasso.load(user.avatar_url).transform(RoundedTransformation).into(avatar)
    }

    override fun doPageRequest(url: String) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Paging search list with $url...")
            val result = gitHubService.searchUsersPaginate(url).awaitResult()
            when (result) {
                is Result.Ok -> appendDataSet(result.value.items)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}