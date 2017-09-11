package com.zenhub.gist

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.zenhub.R
import com.zenhub.core.asDigitalUnit
import com.zenhub.github.mappings.GistFile
import com.zenhub.repo.contents.FileContentsActivity

class GistFilesAdapter(private val ctx: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inflater by lazy { LayoutInflater.from(ctx) }
    private val dataset = mutableListOf<GistFile>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = inflater.inflate(R.layout.gists_file_item, parent, false)
        return object: RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.filename).text = dataset[position].filename
        holder.itemView.findViewById<TextView>(R.id.language).text = dataset[position].language
        holder.itemView.findViewById<TextView>(R.id.size).text = dataset[position].size.asDigitalUnit()
        holder.itemView.setOnClickListener {
            val intent = Intent(ctx, FileContentsActivity::class.java)
            intent.putExtra("FILE_URL", dataset[position].raw_url)
            ContextCompat.startActivity(ctx, intent, null)
        }
    }

    override fun getItemCount() = dataset.size

    fun update(files: Collection<GistFile>) {
        dataset.clear()
        dataset.addAll(files)
        notifyDataSetChanged()
    }

}