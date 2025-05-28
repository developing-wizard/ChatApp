package com.example.chatapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.R
import com.example.chatapp.adapters.MessagesAdaptor
import com.example.chatapp.databinding.ActivityChatBinding
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class ChatActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val usersRef: CollectionReference = db.collection("users_collection")
    private val messagesRef: CollectionReference = db.collection("messages_collection")
    private lateinit var messagesAdaptor: MessagesAdaptor
    private lateinit var messages: MutableList<ChatMessage>
    private lateinit var currentUser: User
    private lateinit var storageRef: StorageReference
    private lateinit var uri: Uri
    private lateinit var binding: ActivityChatBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storageRef = FirebaseStorage.getInstance().reference
        initRecyclerView()
        getCurrentUser()
        binding.sendMessageButton.setOnClickListener { insertMessage() }
        val pickMultipleMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uriImage ->
                if (uriImage != null) {
                    uri = uriImage
                    uploadImage()
                } else {
                    Toast.makeText(this, "Can't Upload  , Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                }

            }

        binding.inputMessage.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_BOTTOM = 3
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.inputMessage.right - binding.inputMessage.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                    binding.inputMessage.setText("")
                    pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            false
        }

    }

    override fun onStart() {
        super.onStart()

        messagesRef.orderBy("timeStamp", Query.Direction.ASCENDING)
            .addSnapshotListener(this) { snapshots, error ->
                error?.let {
                    return@addSnapshotListener
                }

                snapshots?.let {
                    for (dc in it.documentChanges) {
                        val oldIndex = dc.oldIndex
                        val newIndex = dc.newIndex

                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                val snapshot = dc.document
                                val message = snapshot.toObject(ChatMessage::class.java)
                                messages.add(newIndex, message)
                                binding.messageRecyclerView.smoothScrollToPosition(messages.size - 1)
                                Toast.makeText(this, message.image, Toast.LENGTH_LONG).show()
                            }

                            DocumentChange.Type.REMOVED -> {

                            }

                            DocumentChange.Type.MODIFIED -> {

                            }
                        }

                    }
                }
            }
    }

    private fun initRecyclerView() {
        messages = mutableListOf()
        messagesAdaptor = MessagesAdaptor(this@ChatActivity, messages)
        binding.messageRecyclerView.setAdapter(messagesAdaptor)
        binding.messageRecyclerView.setHasFixedSize(true)
        binding.messageRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun getCurrentUser() {
        usersRef.whereEqualTo("id", FirebaseAuth.getInstance().currentUser?.uid)
            .get()
            .addOnSuccessListener {
                for (snapshot in it) {
                    currentUser = snapshot.toObject(User::class.java)
                }
            }
    }

    private fun insertMessage() {
        var message = binding.inputMessage.text.toString()
        if (message.isNotEmpty()) {
            messagesRef.document()
                .set(ChatMessage(currentUser, message, null, ""))
                .addOnCompleteListener {
                    if (it.isComplete) {
                        binding.inputMessage.setText("")
                        message = ""
                    } else {

                    }
                }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_sing_out -> {
                FirebaseAuth.getInstance().signOut()
                Intent(this@ChatActivity, MainActivity::class.java).also {
                    startActivity(it)
                }
                return true
            }
        }
        return false
    }

    private fun uploadImage() {
        if (this::uri.isInitialized) {
            showProgressBar()
            val filePath = storageRef.child("chat_images").child(uri.lastPathSegment!!)
            filePath.putFile(uri).addOnSuccessListener { task ->
                val result: Task<Uri> = task.metadata?.reference?.downloadUrl!!
                result.addOnSuccessListener {
                    uri = it
                    Toast.makeText(this@ChatActivity, it.toString(), Toast.LENGTH_SHORT).show()

                }
                val message = ChatMessage(currentUser, uri.toString())
                messagesRef.document()
                    .set(message)
                    .addOnCompleteListener {
                        if (it.isComplete) {
                            hideProgressBar()
                            Toast.makeText(this, "Image added!", Toast.LENGTH_SHORT).show()
                        } else {
                            hideProgressBar()
                            Toast.makeText(this, "Image wasn't added!", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }


    private fun showProgressBar() {
        binding.progressBarChatAct.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBarChatAct.visibility = View.GONE
    }
}