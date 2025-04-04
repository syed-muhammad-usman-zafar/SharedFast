package com.example.sharedfast

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sharedfast.adapter.FolderAdapter
import com.example.sharedfast.model.Folder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var folderAdapter: FolderAdapter
    private var folderList = mutableListOf<Folder>()
    private var filteredFolderList = mutableListOf<Folder>()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var searchView: SearchView
    private val folderKey = "folderList"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("SharedFastPrefs", MODE_PRIVATE)
        loadFolders()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Create a copy of the folder list for filtering
        filteredFolderList = folderList.toMutableList()

        // Updated adapter with delete functionality and long click for sharing
        folderAdapter = FolderAdapter(
            filteredFolderList,
            { folder -> onFolderClicked(folder) },
            { position -> deleteFolder(position) },
            { folder -> showSharingOptions(folder) } // Added this for sharing
        )

        recyclerView.adapter = folderAdapter

        // Initialize SearchView and set up its functionality
        searchView = findViewById(R.id.search_bar)
        setupSearchView()

        val fabAddFolder: FloatingActionButton = findViewById(R.id.fab_add_folder)
        fabAddFolder.setOnClickListener {
            showFolderCreationDialog()
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterFolders(newText)
                return true
            }
        })
    }

    private fun filterFolders(query: String?) {
        filteredFolderList.clear()

        if (query.isNullOrEmpty()) {
            filteredFolderList.addAll(folderList)
        } else {
            val searchQuery = query.lowercase()
            for (folder in folderList) {
                if (folder.name.lowercase().contains(searchQuery)) {
                    filteredFolderList.add(folder)
                }
            }
        }
        folderAdapter.notifyDataSetChanged()
    }

    private val folderActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val folderName = data.getStringExtra("folder_name") ?: return@let
                val folderImages = data.getStringArrayListExtra("folder_images") ?: return@let

                // Update folder in the list
                val folderIndex = folderList.indexOfFirst { it.name == folderName }
                if (folderIndex != -1) {
                    folderList[folderIndex] = Folder(folderName, folderImages)
                    // Update filtered list if folder is there
                    val filteredIndex = filteredFolderList.indexOfFirst { it.name == folderName }
                    if (filteredIndex != -1) {
                        filteredFolderList[filteredIndex] = Folder(folderName, folderImages)
                        folderAdapter.notifyItemChanged(filteredIndex)
                    }
                    saveFolders()
                }
            }
        }
    }
    private fun onFolderClicked(folder: Folder) {
        // Pass the folder details to the FolderActivity
        val intent = Intent(this, FolderActivity::class.java)
        // You can pass the folder's name and images as extras
        intent.putExtra("folder_name", folder.name)
        intent.putStringArrayListExtra("folder_images", ArrayList(folder.images))
        folderActivityLauncher.launch(intent)
    }

    // New method to show sharing options when a folder is selected (long-pressed)
    private fun showSharingOptions(folder: Folder) {
        if (folder.images.isEmpty()) {
            Toast.makeText(this, "No images to share in ${folder.name}", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a dialog with sharing options
        val options = arrayOf("Facebook", "WhatsApp", "Bluetooth", "Gmail")

        AlertDialog.Builder(this)
            .setTitle("Share ${folder.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareViaApp(folder, "com.facebook.katana")
                    1 -> shareViaApp(folder, "com.whatsapp")
                    2 -> shareViaBluetooth(folder)
                    3 -> shareViaApp(folder, "com.google.android.gm")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Method to share via specific app package
    private fun shareViaApp(folder: Folder, packageName: String) {
        if (folder.images.isEmpty()) {
            Toast.makeText(this, "No images to share", Toast.LENGTH_SHORT).show()
            return
        }

        // Create an intent to share
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/*"

            // Convert string paths to Uri objects
            val imageUris = ArrayList<Uri>()
            for (imagePath in folder.images) {
                imageUris.add(Uri.parse(imagePath))
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)

            putExtra(Intent.EXTRA_SUBJECT, "Sharing images from ${folder.name}")
            putExtra(Intent.EXTRA_TEXT, "Check out these images from SharedFast!")
            setPackage(packageName)
        }

        try {
            startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "App not installed or sharing not supported", Toast.LENGTH_SHORT).show()
            // Fallback to general sharing if specific app isn't available
            val fallbackIntent = Intent.createChooser(shareIntent.setPackage(null), "Share via")
            startActivity(fallbackIntent)
        }
    }

    // Method specifically for Bluetooth sharing
    private fun shareViaBluetooth(folder: Folder) {
        if (folder.images.isEmpty()) {
            Toast.makeText(this, "No images to share", Toast.LENGTH_SHORT).show()
            return
        }

        // Create an intent to share via Bluetooth
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "image/*"

            // Convert string paths to Uri objects
            val imageUris = ArrayList<Uri>()
            for (imagePath in folder.images) {
                imageUris.add(Uri.parse(imagePath))
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)

            // Set Bluetooth package
            setPackage("com.android.bluetooth")
        }

        try {
            startActivity(shareIntent)
        } catch (e: Exception) {
            // Fallback if Bluetooth package isn't available or varies by device
            val fallbackIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                val imageUris = ArrayList<Uri>()
                for (imagePath in folder.images) {
                    imageUris.add(Uri.parse(imagePath))
                }
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
            }
            startActivity(Intent.createChooser(fallbackIntent, "Share via Bluetooth"))
        }
    }

    private fun showFolderCreationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_folder_creation, null)
        val folderNameInput = dialogView.findViewById<TextInputEditText>(R.id.folderNameInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Create New Folder")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val folderName = folderNameInput.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    val newFolder = Folder(folderName, listOf())  // Create folder with no images for now
                    addFolder(newFolder)
                    Toast.makeText(this, "Folder Created: $folderName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Folder name can't be empty!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun addFolder(folder: Folder) {
        folderList.add(folder)
        // Add to filtered list as well if it should be visible
        val query = searchView.query?.toString() ?: ""
        if (query.isEmpty() || folder.name.lowercase().contains(query.lowercase())) {
            filteredFolderList.add(folder)
        }
        folderAdapter.notifyDataSetChanged()
        saveFolders()
    }

    // Updated method to delete a folder
    private fun deleteFolder(position: Int) {
        if (position >= 0 && position < filteredFolderList.size) {
            val folder = filteredFolderList[position]
            val folderName = folder.name

            // Remove from the filtered list
            filteredFolderList.removeAt(position)

            // Also remove from the original list
            folderList.remove(folder)

            folderAdapter.notifyItemRemoved(position)
            saveFolders()
            Toast.makeText(this, "Folder '$folderName' deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFolders() {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(folderList)  // Save the complete list, not the filtered one
        editor.putString(folderKey, json)
        editor.apply()
    }

    private fun loadFolders() {
        val gson = Gson()
        val json = sharedPreferences.getString(folderKey, null)
        val type = object : TypeToken<MutableList<Folder>>() {}.type
        folderList = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }
}