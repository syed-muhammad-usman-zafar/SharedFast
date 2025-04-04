package com.example.sharedfast

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FolderActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var searchView: SearchView
    private var imageList = mutableListOf<ImageItem>()
    private var filteredImageList = mutableListOf<ImageItem>()
    private var folderName: String = ""
    private var currentPhotoPath: String = ""

    // Create a temporary file for camera photos
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_msys", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(
            imageFileName, ".jpg", storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    // Activity result launcher for capturing images
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Save the captured image to MediaStore
            val mediaSavedPath = MediaStoreHelper.saveCapturedImageToMediaStore(
                this, folderName, currentPhotoPath
            )

            if (mediaSavedPath != null) {
                val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val newImage = ImageItem(mediaSavedPath, "Image $timeStamp", timeStamp)
                imageList.add(newImage)

                // Add to filtered list if it matches the current query
                val query = searchView.query?.toString() ?: ""
                if (matchesSearchQuery(newImage, query)) {
                    filteredImageList.add(newImage)
                    imageAdapter.notifyItemInserted(filteredImageList.size - 1)
                }

                saveImagesToPreferences()
            }
        }
    }

    // Activity result launcher for picking images from gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Save the picked image to MediaStore
            val mediaSavedPath = MediaStoreHelper.saveImageToMediaStore(this, folderName, it)

            if (mediaSavedPath != null) {
                val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val newImage = ImageItem(mediaSavedPath, "Image $timeStamp", timeStamp)
                imageList.add(newImage)

                // Add to filtered list if it matches the current query
                val query = searchView.query?.toString() ?: ""
                if (matchesSearchQuery(newImage, query)) {
                    filteredImageList.add(newImage)
                    imageAdapter.notifyItemInserted(filteredImageList.size - 1)
                }

                saveImagesToPreferences()
            }
        }
    }

    // Activity result launcher for picking files
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Save the picked file to MediaStore
            val mediaSavedPath = MediaStoreHelper.saveImageToMediaStore(this, folderName, it)

            if (mediaSavedPath != null) {
                val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val newImage = ImageItem(mediaSavedPath, "File $timeStamp", timeStamp)
                imageList.add(newImage)

                // Add to filtered list if it matches the current query
                val query = searchView.query?.toString() ?: ""
                if (matchesSearchQuery(newImage, query)) {
                    filteredImageList.add(newImage)
                    imageAdapter.notifyItemInserted(filteredImageList.size - 1)
                }

                saveImagesToPreferences()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        // Get folder name from intent
        folderName = intent.getStringExtra("folder_name") ?: "Unnamed Folder"
        val folderImages = intent.getStringArrayListExtra("folder_images") ?: arrayListOf()

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = folderName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Convert the string list to image items
        imageList = folderImages.mapIndexed { index, path ->
            val timeStamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            ImageItem(path, "Image $index", timeStamp)
        }.toMutableList()

        // Initialize filtered list with all images
        filteredImageList = imageList.toMutableList()

        // Initialize SearchView
        searchView = findViewById(R.id.search_bar)
        setupSearchView()

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recyclerViewImages)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        imageAdapter = ImageAdapter(filteredImageList) { position ->
            // Handle image click
            Toast.makeText(this, "Image $position clicked", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = imageAdapter

        // Set up buttons
        findViewById<MaterialButton>(R.id.btnImportFiles).setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        findViewById<MaterialButton>(R.id.btnImportImages).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<MaterialButton>(R.id.btnCaptureImage).setOnClickListener {
            val photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                this,
                "com.example.shared fast.file provider",
                photoFile
            )
            takePictureLauncher.launch(photoURI)
        }


        window.statusBarColor = ContextCompat.getColor(this, R.color.black)
    }

    private fun setupSearchView() {
        searchView.queryHint = "Search Images"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterImages(newText)
                return true
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterImages(query: String?) {
        filteredImageList.clear()

        if (query.isNullOrEmpty()) {
            filteredImageList.addAll(imageList)
        } else {
            for (image in imageList) {
                if (matchesSearchQuery(image, query)) {
                    filteredImageList.add(image)
                }
            }
        }
        imageAdapter.notifyDataSetChanged()
    }

    private fun matchesSearchQuery(image: ImageItem, query: String): Boolean {
        val searchQuery = query.lowercase()
        return image.title.lowercase().contains(searchQuery) ||
                image.date.lowercase().contains(searchQuery)
    }



    // Update the saveImagesToPreferences method
    private fun saveImagesToPreferences() {
        val imagePaths = imageList.map { it.path }

        // Use SharedPreferences directly to save the updated list
        val sharedPreferences = getSharedPreferences("SharedFastPrefs", MODE_PRIVATE)

        // Get the current folder list
        val gson = Gson()
        val json = sharedPreferences.getString("folderList", null)
        val type = object : TypeToken<MutableList<Folder>>() {}.type
        val folderList: MutableList<Folder> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        // Update the specific folder
        val folderIndex = folderList.indexOfFirst { it.name == folderName }
        if (folderIndex != -1) {
            // Update existing folder
            folderList[folderIndex] = Folder(folderName, imagePaths)
        } else {
            // Add new folder
            folderList.add(Folder(folderName, imagePaths))
        }

        // Save back to SharedPreferences
        val editor = sharedPreferences.edit()
        val updatedJson = gson.toJson(folderList)
        editor.putString("folderList", updatedJson)
        editor.apply()

        // Also prepare result intent for when activity finishes
        val resultIntent = Intent()
        resultIntent.putExtra("folder_name", folderName)
        resultIntent.putStringArrayListExtra("folder_images", ArrayList(imagePaths))
        setResult(RESULT_OK, resultIntent)
    }
}