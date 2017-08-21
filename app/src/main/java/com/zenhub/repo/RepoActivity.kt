package com.zenhub.repo

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zenhub.BaseActivity
import com.zenhub.R
import com.zenhub.repo.commits.buildCommitsView
import com.zenhub.repo.contents.buildContentsView

class RepoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.repo_activity)
        super.onCreateDrawer()

        val fullRepoName = intent.getStringExtra("REPO_FULL_NAME")
        supportActionBar?.title = fullRepoName

        val viewPager = findViewById<ViewPager>(R.id.pager)
        viewPager.adapter = RepoDetailsPagerAdapter(this, viewPager, fullRepoName)
        val tabLayout = findViewById<TabLayout>(R.id.tablayout)
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.position, true)
            }
        })

        requestDataRefresh()
    }
}

class RepoDetailsPagerAdapter(context: Activity,
                              container: ViewPager,
                              fullRepoName: String) : PagerAdapter() {

    private val inflater = LayoutInflater.from(context)
    private val readMeView = buildReadmeView(inflater, container, fullRepoName)
    private val commitsView = buildCommitsView(inflater, container, fullRepoName)
    private val contentsView = buildContentsView(inflater, container, fullRepoName)

    override fun isViewFromObject(view: View?, obj: Any?) = view == obj

    override fun getCount() = 3

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = when (position) {
            0 -> readMeView
            1 -> commitsView
            else -> contentsView
        }
        container.addView(layout)
        return layout
    }

    override fun destroyItem(container: ViewGroup?, position: Int, view: Any?) {
        container?.removeView(view as View?)
    }
}
