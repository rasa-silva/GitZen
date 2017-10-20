package com.zenhub.repo.issues

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.zenhub.R
import com.zenhub.core.asFuzzyDate
import com.zenhub.github.Comment
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
        val header = TextView(this)
        header.text = "Comments"
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)
        header.textAlignment = View.TEXT_ALIGNMENT_CENTER
        comments.addHeaderView(header, null, false)

        val adapter = IssueCommentAdapter(this)
        comments.adapter = adapter
        adapter.addAll(issue.comments.nodes)
    }
}

class IssueCommentAdapter(private val ctx: Context) : ArrayAdapter<Comment>(ctx, android.R.layout.simple_list_item_1) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(ctx).inflate(R.layout.repo_issue_comment, parent, false)
        val comment = getItem(position)
        view.findViewById<TextView>(R.id.description).text = comment.body
        view.findViewById<TextView>(R.id.author).text = comment.author.login
        view.findViewById<TextView>(R.id.updated_at).text = comment.createdAt.asFuzzyDate()
        return view
    }
}
