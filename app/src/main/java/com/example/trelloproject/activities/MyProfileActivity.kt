package com.example.trelloproject.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.trelloproject.R
import com.example.trelloproject.databinding.ActivityMyProfileBinding
import com.example.trelloproject.firebase.FirestoreClass
import com.example.trelloproject.models.User
import com.example.trelloproject.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    private var mSelectedImageFileUri:Uri?=null
    private var mProfileImageURL : String =""
    private lateinit var mUserDetails: User
    private lateinit var binding: ActivityMyProfileBinding
    private lateinit var toolbar: Toolbar
    private lateinit var view: ImageView

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        toolbar = binding.toolbarMyProfileActivity
        view = binding.ivProfileUserImage
        setContentView(binding.root)
        setupActionBar()
        FirestoreClass().loadUserData(this)
        binding.ivProfileUserImage.setOnClickListener {
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
        binding.btnUpdate.setOnClickListener{
            if(mSelectedImageFileUri!=null){
                uploadUserImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                updateUserProfileData()
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


    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = resources.getString(R.string.my_profile)
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun setUserDataInUI(user: User) {
        mUserDetails=user
        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(view)
        binding.etName.setText(user.name)
        binding.etEmail.setText(user.email)
        if (user.mobile != 0L) {
            binding.etMobile.setText(user.mobile.toString())
        }
    }

    private fun updateUserProfileData(){
        val userHashMap = HashMap<String,Any>()
        if(mProfileImageURL.isNotEmpty() && mProfileImageURL!=mUserDetails.image){
            userHashMap[Constants.IMAGE]=mProfileImageURL
        }
        if(binding.etName.text.toString()!=mUserDetails.name){
            userHashMap[Constants.NAME]=binding.etName.text.toString()
        }
        if(binding.etMobile.text.toString()!=mUserDetails.mobile.toString()){
            userHashMap[Constants.MOBILE]=binding.etMobile.text.toString().toLong()
        }
        FirestoreClass().updateUserProfileData(this,userHashMap)

    }

    private fun uploadUserImage(){
        showProgressDialog(resources.getString(R.string.please_wait))
        if(mSelectedImageFileUri!=null){
            val sRef : StorageReference =
                FirebaseStorage.getInstance().
                reference.child("USER_IMAGE" +
                        System.currentTimeMillis()+ "." +
                        getFileExtension(mSelectedImageFileUri))
            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {
                taskSnapshot->
                    Log.i(
                        "Firebase Image URL",
                        taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                    )
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                    Log.i("Downloadable Image URL",uri.toString())
                    mProfileImageURL=uri.toString()
                    updateUserProfileData()
                }
            }.addOnFailureListener {
                exception ->
                Toast.makeText(this@MyProfileActivity,exception.message,Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }
        }
    }
    private fun getFileExtension(uri:Uri?):String?{
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
    }
    fun profileUpdateSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }
}
