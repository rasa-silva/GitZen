package com.zenhub.gist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import com.zenhub.R
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.NewGist
import com.zenhub.showErrorOnSnackbar
import com.zenhub.showExceptionOnSnackbar
import kotlinx.android.synthetic.main.activity_new_gist.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

class NewGistActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_gist)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.new_gist_options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_send -> {
                createGist()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createGist() {
        if (TextUtils.isEmpty(description.text)) {
            description.error = "Must not be empty."
            return
        }

        if (TextUtils.isEmpty(content.text)) {
            content.error = "Must not be empty."
            return
        }

        val fileContents = "content" to content.text.toString()
        val fileList = mapOf("file1.txt" to mapOf(fileContents))
        val newGist = NewGist(description.text.toString(), publicOrPrivate.isChecked, fileList)

        launch(UI) {
            val result = gitHubService.createGist(newGist).awaitResult()
            when (result) {
                is Result.Ok -> {
                    setResult(Activity.RESULT_OK, Intent().putExtra("GIST", result.value))
                    finish()
                }
                is Result.Error -> showErrorOnSnackbar(new_gist, result.response.message())
                is Result.Exception -> showExceptionOnSnackbar(new_gist, result.exception)
            }
        }
    }
}
