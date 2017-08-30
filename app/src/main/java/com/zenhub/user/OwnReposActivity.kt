package com.zenhub.user

import com.zenhub.github.Repository
import com.zenhub.github.gitHubService
import retrofit2.Call

class OwnReposActivity : RepoListActivity() {

    override fun requestInitial(): Call<List<Repository>> {
        return gitHubService.listRepos()
    }

    override fun requestPage(url: String): Call<List<Repository>> {
        return gitHubService.listReposPaginate(url)
    }
}