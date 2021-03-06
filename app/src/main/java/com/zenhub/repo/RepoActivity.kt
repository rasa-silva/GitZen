package com.zenhub.repo

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.zenhub.R
import com.zenhub.repo.commits.CommitsFragment
import com.zenhub.repo.contents.ContentsFragment
import com.zenhub.repo.issues.IssuesFragment

class RepoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fullRepoName = intent.getStringExtra("REPO_FULL_NAME")
        supportActionBar?.title = fullRepoName

        val viewPager = findViewById<ViewPager>(R.id.pager)
        viewPager.adapter = RepoDetailsPagerAdapter(fullRepoName, supportFragmentManager)
        val tabLayout = findViewById<TabLayout>(R.id.tablayout)
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position, true)
            }
        })
    }
}

class RepoDetailsPagerAdapter(private val fullRepoName: String, fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val about by lazy { AboutFragment() }
    private val commits by lazy { CommitsFragment() }
    private val issues by lazy { IssuesFragment() }
    private val contents by lazy { ContentsFragment() }

    override fun getItem(position: Int): Fragment {
        val fragment = when (position) {
            0 -> about
            1 -> commits
            2 -> issues
            else -> contents
        }

        val args = Bundle()
        args.putString("REPO_NAME", fullRepoName)
        fragment.arguments = args
        return fragment
    }

    override fun getCount() = 4
}
