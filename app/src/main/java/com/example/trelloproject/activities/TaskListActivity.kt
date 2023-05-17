package com.example.trelloproject.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trelloproject.R
import com.example.trelloproject.adapters.TaskListItemsAdapter
import com.example.trelloproject.databinding.ActivityTaskListBinding
import com.example.trelloproject.firebase.FirestoreClass
import com.example.trelloproject.models.Board
import com.example.trelloproject.models.Card
import com.example.trelloproject.models.Task
import com.example.trelloproject.models.User
import com.example.trelloproject.utils.Constants

class TaskListActivity : BaseActivity() {
    private lateinit var binding: ActivityTaskListBinding
    private lateinit var mBoardDetails : Board
    private lateinit var mBoardDocumentId : String
    private lateinit var membersLauncher: ActivityResultLauncher<Intent>
    private lateinit var cardDetailsLauncher: ActivityResultLauncher<Intent>
    lateinit var mAssignedMemberDetailsList : ArrayList<User>
    override fun onCreate(savedInstanceState: Bundle?) {
        binding=ActivityTaskListBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (intent.hasExtra(Constants.DOCUMENT_ID)){
            mBoardDocumentId=intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }
        membersLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getBoardDetails(this, mBoardDocumentId)
            } else {
                Log.e("Cancelled", "Cancelled")
            }
        }
        cardDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getBoardDetails(this, mBoardDocumentId)
            } else {
                Log.e("Cancelled", "Cancelled")
            }
        }
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this,mBoardDocumentId)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_members -> {
                val intent = Intent(this, MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
                membersLauncher.launch(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarTaskListActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = mBoardDetails.name
        }
        binding.toolbarTaskListActivity.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    fun boardDetails(board: Board){
        mBoardDetails=board
        hideProgressDialog()
        setupActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this,mBoardDetails.assignedTo)
    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this,mBoardDetails.documentId)
    }

    fun createTaskList(taskListName : String){
        val task = Task(taskListName,FirestoreClass().getCurrentUserId())
        mBoardDetails.taskList.add(0,task)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    fun updateTaskList(position:Int, listName:String, model:Task){
        val task=Task(listName,model.createdBy)
        mBoardDetails.taskList[position]=task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }
    fun deleteTaskList(position: Int){
        mBoardDetails.taskList.removeAt(position)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }
    fun addCardToList(position: Int , cardName : String){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        val cardAssignedUserList : ArrayList<String> = ArrayList()
        cardAssignedUserList.add(FirestoreClass().getCurrentUserId())
        val card = Card(cardName,FirestoreClass().getCurrentUserId(),cardAssignedUserList)
        val cardsList = mBoardDetails.taskList[position].cards
        cardsList.add(card)
        val task = Task(
            mBoardDetails.taskList[position].title,
            mBoardDetails.taskList[position].createdBy,
            cardsList
        )
        mBoardDetails.taskList[position]=task
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    fun cardDetails(taskListPosition: Int, cardPosition : Int){
        val intent = Intent(this,CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAIL,mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION,taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION,cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST, mAssignedMemberDetailsList)
        cardDetailsLauncher.launch(intent)
    }
    fun boardMembersDetailsList(list : ArrayList<User>){
        mAssignedMemberDetailsList=list
        hideProgressDialog()
        val addTaskList= Task(resources.getString(R.string.add_list))
        mBoardDetails.taskList.add(addTaskList)
        binding.rvTaskList.layoutManager=LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        binding.rvTaskList.setHasFixedSize(true)
        val adapter=TaskListItemsAdapter(this,mBoardDetails.taskList)
        binding.rvTaskList.adapter=adapter
    }
    fun updateCardsInTaskList(taskListPosition: Int,cards : ArrayList<Card>){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        mBoardDetails.taskList[taskListPosition].cards=cards
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }
}