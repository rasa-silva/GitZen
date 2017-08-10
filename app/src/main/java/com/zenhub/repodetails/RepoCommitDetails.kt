package com.zenhub.repodetails

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.widget.TextView
import com.zenhub.Application
import com.zenhub.BaseActivity
import com.zenhub.R
import com.zenhub.github.GitHubApi

class RepoCommitDetails : BaseActivity() {

    lateinit var repoName: String
    lateinit var commitSha: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_commit_activity)
        super.onCreateDrawer()

        repoName = intent.getStringExtra("REPO_FULL_NAME")
        commitSha = intent.getStringExtra("COMMIT_SHA")

        requestDataRefresh()
    }

    override fun requestDataRefresh() {
        Log.d(Application.LOGTAG, "Refreshing commit details...")
        val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        GitHubApi.commitDetails(repoName, commitSha, refreshLayout) { response, _ ->
            response?.let {
                val message = refreshLayout.findViewById<TextView>(R.id.commit_message)
                message.text = response.commit.message
                val sha = refreshLayout.findViewById<TextView>(R.id.commit_sha)
                sha.text = commitSha
            }
            refreshLayout.isRefreshing = false
        }
    }
}