package com.zenhub.gist

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.zenhub.R
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.NewGist
import com.zenhub.showErrorOnSnackbar
import com.zenhub.showInfoOnSnackbar
import kotlinx.android.synthetic.main.activity_new_gist.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.awaitResponse

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
        launch(UI) {
            val description = description.text.toString()
            val fileContents = "content" to content.text.toString()
            val fileList = mapOf("file1.txt" to mapOf(fileContents))
            val newGist = NewGist(description, publicOrPrivate.isChecked, fileList)
            val response = gitHubService.createGist(newGist).awaitResponse()
            when {
                response.isSuccessful -> showInfoOnSnackbar(new_gist, "Gist created.")
                else -> showErrorOnSnackbar(new_gist, response.message())
            }
        }
    }
}
