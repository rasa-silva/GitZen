package com.zenhub.gist

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
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.Gist
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class GistListAdapter(private val recyclerView: RecyclerView) : PagedRecyclerViewAdapter<Gist?>(recyclerView.context, R.layout.gists_item) {

    override fun bindData(itemView: View, model: Gist?) {
        val gist = model ?: return

        val description = if (gist.description.isNullOrBlank()) {
            gist.files.keys.elementAt(0)
        } else gist.description

        itemView.findViewById<TextView>(R.id.description).text = description
        itemView.findViewById<TextView>(R.id.updated_at).text = gist.updated_at.asFuzzyDate()

        itemView.setOnClickListener {
            val intent = Intent(Application.context, GistDetailsActivity::class.java)
            intent.putExtra("URL", gist.url)
            ContextCompat.startActivity(Application.context, intent, null)
        }
    }

    override fun doPageRequest(url: String) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Paging gist list with $url...")
            val result = gitHubService.listGistsPaginate(url).awaitResult()
            when (result) {
                is Result.Ok -> appendDataSet(result.value)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}