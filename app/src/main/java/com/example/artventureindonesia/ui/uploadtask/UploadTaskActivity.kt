package com.example.artventureindonesia.ui.uploadtask

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.example.artventureindonesia.R
import com.example.artventureindonesia.databinding.ActivityUploadTaskBinding
import com.example.artventureindonesia.remote.response.MLResponse
import com.example.artventureindonesia.ui.viewmodel.ViewModelFactory
import com.example.artventureindonesia.remote.result.Result
import com.example.artventureindonesia.ui.detailtask.DetailTaskActivity
import com.example.artventureindonesia.ui.task.TaskActivity
import com.example.artventureindonesia.utils.getImageUri
//import com.example.artventureindonesia.ui.viewmodel.ViewModelFactory
import com.example.artventureindonesia.utils.reduceFileImage
import com.example.artventureindonesia.utils.uriToFile

class UploadTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadTaskBinding
    private var currentImageUri: Uri? = null

    private val viewModel by viewModels<UploadTaskViewModel> {
        ViewModelFactory.getInstance(this)
    }

    private var objectDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        objectDocId = intent.getStringExtra(ID)

        Log.d("UploadTaskActivityy", "Received objectDocId: $objectDocId")

        binding.btnGallery.setOnClickListener { startGallery() }
        binding.btnCamera.setOnClickListener { startCamera() }
        binding.btnUpload.setOnClickListener { uploadImage() }

    }

    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri)
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }


    private fun uploadImage(){
        viewModel.getSession().observe(this) { user ->
            currentImageUri?.let { uri ->
                val image = uriToFile(uri, this).reduceFileImage()
                Log.d("Image File", "showImage: ${image.path}")
//                objectDocId?.let { Log.d("IDddd", it) }

                objectDocId?.let {
                    viewModel.uploadImage(image, it).observe(this) { result ->
                        if (result != null) {
                            when (result) {
                                is Result.Loading -> {
                                    showLoading(true)
                                }

                                is Result.Success -> {
                                    //                                showToast(result.data.message!!)
                                    showLoading(false)
//                                    alertDialog(result.data.message)
                                    handleUploadResult(result.data)
                                }

                                is Result.Error -> {
                                    showToast(result.error)
                                    showLoading(false)
                                    val intent = Intent(this, DetailTaskActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                    }
                }


                showLoading(true)
            } ?: showToast(getString(R.string.empty_image))
        }
    }


    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.imagePreview.setImageURI(it)
        }
    }

    private fun handleUploadResult(response: MLResponse) {
        if (response.result == "Gagal") {
            alertDialog("Kamu salah mengerjakan task", stayOnPage = true)
        } else {
            alertDialog(response.message ?: getString(R.string.congrats), stayOnPage = false)
        }
    }

    private fun alertDialog(message: String, stayOnPage: Boolean) {
        AlertDialog.Builder(this).apply {
            setTitle(if (stayOnPage) "error" else getString(R.string.congrats))
            setMessage(message)
            setPositiveButton(getString(R.string.ok)) { _, _ ->
                if (!stayOnPage) {
                    val intent = Intent(context, TaskActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
            create()
            show()
        }
    }

//    private fun alertDialog(message: String) {
//        AlertDialog.Builder(this).apply {
//            val title = getString(R.string.congrats)
//            val storyUploaded = getString(R.string.image_uploaded)
//            val ok = getString(R.string.ok)
//
//            setTitle(title)
//            setMessage(storyUploaded)
//            setPositiveButton(ok) { _, _ ->
//                val intent = Intent(context, TaskActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
//                startActivity(intent)
//                finish()
//            }
//            create()
//            show()
//        }
//    }

    companion object {
        const val ID = "object_doc"
    }

}