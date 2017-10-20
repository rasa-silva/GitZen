package com.zenhub.repo.issues

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.zenhub.R
import com.zenhub.github.Issue
import com.zenhub.github.IssueState

class IssueDetails : AppCompatActivity() {

    private val owner by lazy { intent.getStringExtra("OWNER") }
    private val repo by lazy { intent.getStringExtra("REPO") }
    private val issue by lazy { intent.getSerializableExtra("ISSUE") as Issue }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_issue_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "#${issue.number} of $owner / $repo"

        val number = findViewById<TextView>(R.id.number)
        number.text = issue.number.toString()

        val title = findViewById<TextView>(R.id.title)
        title.text = issue.title

        val state = findViewById<TextView>(R.id.state)
        state.text = issue.state.name
        state.background = when (issue.state) {
            IssueState.OPEN -> getDrawable(R.color.colorPrimary)
            IssueState.CLOSED -> getDrawable(R.color.errorBackground)
        }

        val body = findViewById<TextView>(R.id.body)
        body.text = issue.body

        val comments = findViewById<ListView>(R.id.comments)

        val elements = issue.comments.nodes.map { it.body }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, elements)
        comments.adapter = adapter

    }

}