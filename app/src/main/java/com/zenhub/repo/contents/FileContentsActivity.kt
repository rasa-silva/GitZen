package com.zenhub.repo.contents

import android.graphics.Color
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import com.pddstudio.highlightjs.HighlightJsView
import com.pddstudio.highlightjs.models.Language
import com.pddstudio.highlightjs.models.Theme
import com.zenhub.Application
import com.zenhub.BaseActivity
import com.zenhub.R
import java.net.URL

class FileContentsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_contents)
        super.onCreateDrawer()

        val codeView = findViewById<HighlightJsView>(R.id.highlight_view)
        val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
        val urlString = intent.getStringExtra("FILE_URL")

        val substringAfterLast = urlString.substringAfterLast('/')
        supportActionBar?.title = substringAfterLast

        refreshLayout.setOnRefreshListener {
            dataRefresh(codeView, urlString)
        }

        with(codeView) {
            theme = Theme.TOMORROW_NIGHT_EIGHTIES
            setShowLineNumbers(true)
            highlightLanguage = Language.AUTO_DETECT
            setZoomSupportEnabled(true)
            setBackgroundColor(Color.TRANSPARENT)

            setOnContentChangedListener {
                refreshLayout.isRefreshing = false
            }
        }

        dataRefresh(codeView, urlString)
    }

    private fun dataRefresh(codeView: HighlightJsView, urlString: String) {
        Log.d(Application.LOGTAG, "Refreshing file content...")
        codeView.setSource(URL(urlString))
    }
}
