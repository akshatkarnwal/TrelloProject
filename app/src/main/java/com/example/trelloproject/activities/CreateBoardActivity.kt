package com.example.trelloproject.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.trelloproject.R
import com.example.trelloproject.utils.Constants
import com.example.trelloproject.databinding.ActivityCreateBoardBinding
import com.example.trelloproject.firebase.FirestoreClass
import com.example.trelloproject.models.Board
import com.google.common.io.Files.getFileExtension
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class CreateBoardActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateBoardBinding
    private lateinit var toolbar: Toolbar
    private var mSelectedImageFileUri: Uri?=null
    private lateinit var view: ImageView
    private lateinit var mUserName : String
    private var mBoardImageURL : String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            view.setImageURI(imageUri)
            mSelectedImageFileUri = imageUri
            try {
                Glide
                    .with(this)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(view)
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImage.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
            } else {
                Toast.makeText(
                    this,
                    "Oops, you just denied the permission for storage. You can also allow it from settings.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityCreateBoardBinding.inflate(layoutInflater)
        toolbar=binding.toolbarCreateBoardActivity
        view = binding.ivBoardImage
        setContentView(binding.root)
        setupActionBar()
        if (intent.hasExtra(Constants.NAME)){
            mUserName= intent.getStringExtra(Constants.NAME)!!
        }else {
            Toast.makeText(this, "Missing required extra: Constants.NAME", Toast.LENGTH_SHORT).show()
            finish()
        }
        binding.ivBoardImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImage.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }
        binding.btnCreate.setOnClickListener{
            if(mSelectedImageFileUri!=null){
                uploadBoardImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                createBoard()
            }
        }
    }

    private fun createBoard() {
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserID())
        val board= Board(
            binding.etBoardName.text.toString(),
            mBoardImageURL,
            mUserName,
            assignedUsersArrayList
        )
        FirestoreClass().createBoard(this@CreateBoardActivity,board)
    }

    private fun uploadBoardImage(){
        showProgressDialog(resources.getString(R.string.please_wait))
        val sRef : StorageReference =
            FirebaseStorage.getInstance().
            reference.child("BOARD_IMAGE" +
                    System.currentTimeMillis()+ "." +
                    getFileExtension(mSelectedImageFileUri.toString()))
        sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot->
            Log.i(
                "board Image URL",
                taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
            )
            taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                Log.i("Downloadable Image URL",uri.toString())
                mBoardImageURL = uri.toString()
                createBoard()
            }
        }.addOnFailureListener {
                exception ->
            Toast.makeText(this,exception.message,Toast.LENGTH_SHORT).show()
            hideProgressDialog()
        }
    }

    fun boardCreatedSuccessfully(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        Constants.flag=true
        finish()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title=resources.getString(R.string.create_board_title)
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}