package com.zenhub.user

import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.core.PagedRecyclerViewAdapter
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.getLanguageColor
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.Repository
import com.zenhub.repo.RepoActivity
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class RepoListAdapter(private val recyclerView: RecyclerView,
                      private val listType: RepoListType) : PagedRecyclerViewAdapter<Repository?>(recyclerView.context, R.layout.repo_list_item) {
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
            val intent = Intent(Application.context, RepoActivity::class.java)
            intent.putExtra("REPO_FULL_NAME", repoNameView.text.toString())
            ContextCompat.startActivity(Application.context, intent, null)
        }
    }

    override fun doPageRequest(url: String) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Paging own repo list with $url...")
            val result = when (listType) {
                RepoListType.OWN -> gitHubService.listReposPaginate(url).awaitResult()
                RepoListType.STARRED -> gitHubService.listStarredPaginate(url).awaitResult()
                RepoListType.SEARCHED -> {
                    val res = gitHubService.searchReposPaginate(url).awaitResult()
                    when (res) {
                        is Result.Ok -> Result.Ok(res.value.items, res.response)
                        is Result.Error -> Result.Error(res.exception, res.response)
                        is Result.Exception -> Result.Exception(res.exception)
                    }
                }
            }

            when (result) {
                is Result.Ok -> appendDataSet(result.value)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}