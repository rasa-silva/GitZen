package com.zenhub.github

class TokenRequest(val client_id: String, val client_secret: String, val scopes: List<String>, val note: String)
class TokenResponse(val token: String)

class Repository(val name: String, val full_name: String,
                 val description: String, val pushed_at: String,
                 val stargazers_count: Int, val language: String?)

class RepositoryDetails(val name: String, val full_name: String,
                        val description: String, val pushed_at: String,
                        val stargazers_count: Int, val language: String)

class User(val login: String, val avatar_url: String, val name: String,
           val public_repos: Int, val public_gists: Int,
           val followers: Int, val following: Int,
           val created_at: String)

class Committer(val login: String, val avatar_url: String)
class CommitInfo(val message: String, val committer: CommitCommitter)
class CommitCommitter(val name: String, val date: String)
class Commit(val sha: String, val commit: CommitInfo, val committer: Committer?)

class CommitFile(val filename: String, val patch: String)
class CommitDetails(val commit: CommitInfo, val files: List<CommitFile>)

class RepoContentEntry(val name: String, val path: String, val size: Int, val type: String, val download_url: String)

class ErrorMessage(val message: String)
