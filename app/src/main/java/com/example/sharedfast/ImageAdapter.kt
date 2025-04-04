package com.example.sharedfast

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class ImageAdapter(
    private val imageList: MutableList<ImageItem>,
    private val onImageDelete: (Int) -> Unit,
    private val onImageClick: (Int) -> Unit) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>()
{

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val titleTextView: TextView = itemView.findViewById(R.id.imageTitleTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.imageDateTextView)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
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


        // Set up delete button click listener
        holder.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(holder.itemView.context, position)
        }
    }
    private fun showDeleteConfirmationDialog(context: Context, position: Int) {
        val folderName = imageList[position].path
        AlertDialog.Builder(context)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete '$folderName'? This action cannot be undone.")
            .setPositiveButton("Delete")
            { _, _ -> onImageDelete(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemCount(): Int = imageList.size

    // Method to update the image list for searching
    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<ImageItem>) {
        imageList.clear()
        imageList.addAll(newList)
        notifyDataSetChanged()
    }


    // Method to remove a image from the list
    fun removeFolder(position: Int) {
        if (position >= 0 && position < imageList.size) {
            imageList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}