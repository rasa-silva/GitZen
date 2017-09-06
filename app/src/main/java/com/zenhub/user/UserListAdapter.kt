package com.zenhub.user

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.R
import com.zenhub.RoundedTransformation
import com.zenhub.core.PagedRecyclerViewAdapter
import com.zenhub.github.User
import com.zenhub.github.gitHubService
import com.zenhub.showErrorOnSnackbar
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class UserListAdapter(private val recyclerView: RecyclerView,
                      private val listType: UserListType) : PagedRecyclerViewAdapter<User?>(recyclerView.context, R.layout.user_item) {

    override fun bindData(itemView: View, model: User?) {
        val user = model ?: return

        itemView.findViewById<TextView>(R.id.login).text = user.login
        val avatar = itemView.findViewById<ImageView>(R.id.avatar)
        Application.picasso.load(user.avatar_url).transform(RoundedTransformation).into(avatar)
    }

    override fun doPageRequest(url: String) {
        launch(UI) {
            Log.d(Application.LOGTAG, "Paging search list with $url...")
            val result = when (listType) {
                UserListType.SEARCHED -> {
                    val res = gitHubService.searchUsersPaginate(url).awaitResult()
                    when (res) {
                        is Result.Ok -> Result.Ok(res.value.items, res.response)
                        is Result.Error -> Result.Error(res.exception, res.response)
                        is Result.Exception -> Result.Exception(res.exception)
                    }
                }
                else -> gitHubService.listUsersPaginate(url).awaitResult()
            }



            when (result) {
                is Result.Ok -> appendDataSet(result.value)
                is Result.Error -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
                is Result.Exception -> showErrorOnSnackbar(recyclerView, result.exception.localizedMessage)
            }
        }
    }
}