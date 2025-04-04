package com.example.sharedfast.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sharedfast.R
import com.example.sharedfast.model.ImageItem
import com.squareup.picasso.Picasso

class ImageAdapter(
    private val imageList: MutableList<ImageItem>,
    private val onImageClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val titleTextView: TextView = itemView.findViewById(R.id.imageTitleTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.imageDateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = imageList[position]
        holder.titleTextView.text = image.title
        holder.dateTextView.text = image.date

        try {
            // Try to load the image using Picasso
            Picasso.get().load(Uri.parse(image.path)).into(holder.imageView)
        } catch (e: Exception) {
            // If it's not a valid URI, try to load it as a file path
            Picasso.get().load(image.path).error(R.drawable.placeholder_image).into(holder.imageView)
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            onImageClick(position)
        }
    }

    override fun getItemCount(): Int = imageList.size

    // Method to update the image list for searching
    fun updateList(newList: List<ImageItem>) {
        imageList.clear()
        imageList.addAll(newList)
        notifyDataSetChanged()
    }
}