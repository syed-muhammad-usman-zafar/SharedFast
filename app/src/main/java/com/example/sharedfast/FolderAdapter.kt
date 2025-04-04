package com.example.sharedfast

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class FolderAdapter(
    private val folderList: MutableList<Folder>,
    private val onFolderClick: (Folder) -> Unit,
    private val onFolderDelete: (Int) -> Unit,
    private val onFolderLongClick: (Folder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val folderImageView: ImageView = itemView.findViewById(R.id.folderImageView)
        val folderNameTextView: TextView = itemView.findViewById(R.id.folderNameTextView)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folderList[position]
        holder.folderNameTextView.text = folder.name

        // Check if there are images in the folder before loading
        if (folder.images.isNotEmpty()) {
            Picasso.get().load(folder.images[0]).into(holder.folderImageView)
        } else {
            holder.folderImageView.setImageResource(R.drawable.placeholder_image)
        }

        // Set the click listener on the folder
        holder.itemView.setOnClickListener {
            // Call the onFolderClick lambda when a folder is clicked
            onFolderClick(folder)
        }

        // Set up delete button click listener
        holder.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(holder.itemView.context, position)
        }

        // Set up long click listener for sharing
        holder.itemView.setOnLongClickListener {
            onFolderLongClick(folder)
            true
        }
    }

    private fun showDeleteConfirmationDialog(context: Context, position: Int) {
        val folderName = folderList[position].name
        AlertDialog.Builder(context)
            .setTitle("Delete Folder")
            .setMessage("Are you sure you want to delete '$folderName'? This action cannot be undone.")
            .setPositiveButton("Delete")
            { _, _ -> onFolderDelete(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemCount(): Int = folderList.size

    // Method to add a folder to the list dynamically
    fun addFolder(folder: Folder) {
        folderList.add(folder)
        notifyItemInserted(folderList.size - 1)
    }

    // Method to remove a folder from the list
    fun removeFolder(position: Int) {
        if (position >= 0 && position < folderList.size) {
            folderList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}