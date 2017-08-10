package com.zenhub.github

class Repository(val name: String, val full_name: String,
                 val description: String, val pushed_at: String,
                 val stargazers_count: Int, val language: String)

class RepositoryDetails(val name: String, val full_name: String,
                        val description: String, val pushed_at: String,
                        val stargazers_count: Int, val language: String)

class User(val login: String, val avatar_url: String, val name: String,
           val public_repos: Int, val public_gists: Int,
           val followers: Int, val following: Int,
           val created_at: String)

class Committer(val login: String, val avatar_url: String)
class CommitInfo(val message: String, val comment_count: Int, val committer: CommitCommitter)
class CommitCommitter(val date: String)
class Commit(val sha: String, val commit: CommitInfo, val committer: Committer?)

class RepoContentEntry(val name: String, val path: String, val size: Int, val type: String)

class ErrorMessage(val message: String)
