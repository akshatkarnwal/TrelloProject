package com.example.trelloproject.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trelloproject.R
import com.example.trelloproject.adapters.BoardItemsAdapter
import com.example.trelloproject.databinding.ActivityMainBinding
import com.example.trelloproject.firebase.FirestoreClass
import com.example.trelloproject.models.Board
import com.example.trelloproject.models.User
import com.example.trelloproject.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar
    private lateinit var view: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var fabCreateBoard:FloatingActionButton
    private lateinit var mUserName : String
    private lateinit var rvBoardsList: RecyclerView
    private lateinit var tvNoBoardsAvailable: TextView
    private lateinit var mSharedPreferences: SharedPreferences

    private val myProfileActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK ) {
            FirestoreClass().loadUserData(this)
        }else {
            Log.i("MainActivity.kt", "Action canceled by user")
        }
    }

    override fun onStart() {
        super.onStart()
        if(Constants.flag){
            FirestoreClass().getBoardsList(this)
            Constants.flag=false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolbar = findViewById(R.id.toolbar_main_activity)
        view = binding.navView.getHeaderView(0).findViewById(R.id.iv_user_image)
        tvUserName = binding.navView.getHeaderView(0).findViewById(R.id.tv_username)
        fabCreateBoard=findViewById(R.id.fab_create_board)
        rvBoardsList=findViewById(R.id.rv_boards_list)
        tvNoBoardsAvailable=findViewById(R.id.tv_no_boards_available)
        setupActionBar()
        binding.navView.setNavigationItemSelectedListener(this)
        mSharedPreferences=this.getSharedPreferences(Constants.TRELLO_PROJECT_PREFERENCES, Context.MODE_PRIVATE)
        val tokenUpdated=mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED,false)
        if(tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this,true)
        }else{
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener(this@MainActivity) {
                    updateFCMToken(it)
                }
        }
        FirestoreClass().loadUserData(this,true)
        fabCreateBoard.setOnClickListener {
            val intent=Intent(this@MainActivity,CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME,mUserName)
            myProfileActivityResult.launch(intent)
        }
    }

    fun populateBoardListToUI(boardsList : ArrayList<Board>){
        hideProgressDialog()
        if(boardsList.size > 0){
            rvBoardsList.visibility=View.VISIBLE
            tvNoBoardsAvailable.visibility=View.GONE
            rvBoardsList.layoutManager=LinearLayoutManager(this@MainActivity)
            rvBoardsList.setHasFixedSize(true)
            val adapter=BoardItemsAdapter(this,boardsList)
            rvBoardsList.adapter=adapter
            adapter.setOnClickListener(object : BoardItemsAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent=Intent(this@MainActivity,TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID,model.documentId)
                    startActivity(intent)
                }
            })
        }else{
            rvBoardsList.visibility=View.GONE
            tvNoBoardsAvailable.visibility=View.VISIBLE
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        toolbar.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    private fun toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }

    fun updateNavigationUserDetails(user: User, readBoardsList : Boolean) {
        hideProgressDialog()
        mUserName=user.name
        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(view)
        tvUserName.text = user.name
        if (readBoardsList){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this)
        }
        fabCreateBoard.setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUserName)
            startActivity(intent)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_profile -> {
                val intent = Intent(this, MyProfileActivity::class.java)
                myProfileActivityResult.launch(intent)
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()// reset the shared preferences to empty
                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    fun tokenUpdateSuccess(){
        hideProgressDialog()
        val editor : SharedPreferences.Editor=mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED,true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this,true)
    }
    private fun updateFCMToken(token : String){
        val userHashMap = HashMap<String,Any>()
        userHashMap[Constants.FCM_TOKEN]=token
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this,userHashMap)
    }
}