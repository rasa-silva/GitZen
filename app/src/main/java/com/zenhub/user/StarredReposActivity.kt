package com.zenhub.user

import com.zenhub.github.Repository
import com.zenhub.github.gitHubService
import retrofit2.Call

class StarredReposActivity : RepoListActivity() {

    override fun requestInitial(): Call<List<Repository>> {
        return gitHubService.listStarred()
    }

    override fun requestPage(url: String): Call<List<Repository>> {
        return gitHubService.listStarredPaginate(url)
    }
}
