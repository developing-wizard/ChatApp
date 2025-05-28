package com.example.chatapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.R
import com.example.chatapp.databinding.ActivityMainBinding
import com.example.chatapp.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var uri: Uri
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersRef: CollectionReference = db.collection("users_collection")
    private lateinit var storageRef: StorageReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
     binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageRef = FirebaseStorage.getInstance().reference
        val pickMultipleMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uriImage ->
                if (uriImage != null) {
                    binding.profileImage.setImageURI(uriImage)
                    uri = uriImage
                } else {
                    Toast.makeText(this,"Can't Upload  , Something went wrong",Toast.LENGTH_SHORT).show()
                }

            }

        binding.signInButton.setOnClickListener{
            signIn()
        }

        binding.signUpButton.setOnClickListener{
            createAccount()
        }

        binding.profileImage.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))


        }
        binding.textViewRegister.setOnClickListener {
            startNextAnimation()
        }

        binding.textViewSignIn.setOnClickListener {
            startPreviousAnimation()
        }

        binding.textViewGoToProfile.setOnClickListener {
            startNextAnimation()
        }

        binding.textViewSignUp.setOnClickListener {
            startPreviousAnimation()
        }


    }

    private fun signIn() {
        val email = binding.singInInputEmail.text.toString().trim()
        val password = binding.singInInputPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()){
            Toast.makeText(this,"Email or Password missing",Toast.LENGTH_SHORT).show()
        return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password).addOnCompleteListener(this){
            task->
            if (task.isSuccessful){
                sendToAct()
                Toast.makeText(this,"Login Success",Toast.LENGTH_SHORT).show()

            }else{
                Toast.makeText(this,"Something went wrong",Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun createAccount() {
        showProgressBar2()
        val email = binding.singUpInputEmail.text.toString().trim()
        val password = binding.singUpInputPassword.text.toString().trim()
        val confirmPassword = binding.singUpInputConfirmPassword.text.toString().trim()
        val userName = binding.singUpInputUsername.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "You should provide an email and a password", Toast.LENGTH_LONG)
                .show()
            hideProgressBar2()
            return
        }
        if (userName.isEmpty()) {
            Toast.makeText(this, "You should provide an username", Toast.LENGTH_LONG)
                .show()
            hideProgressBar2()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match.", Toast.LENGTH_LONG).show()
            hideProgressBar2()
            return
        }
        if (password.length <= 6) {
            Toast.makeText(this, "Password should have 6 characters or more.", Toast.LENGTH_LONG).show()
            hideProgressBar2()
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created.", Toast.LENGTH_LONG).show()

                    if (task.isComplete) {
                        if (this::uri.isInitialized) {
                            val filePath = storageRef.child("profile_images").child(uri.lastPathSegment!!)
                            filePath.putFile(uri).addOnSuccessListener { task ->
                                val result: Task<Uri> = task.metadata?.reference?.downloadUrl!!
                                result.addOnSuccessListener {
                                    uri = it
                                }
                                val user = User(userName,uri.toString(),FirebaseAuth.getInstance().currentUser?.uid!!)
                                usersRef.document()
                                    .set(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@MainActivity,"Account created",Toast.LENGTH_LONG).show()
                                        hideProgressBar2()
                                        sendToAct()
                                    }.addOnFailureListener {
                                        Toast.makeText(this@MainActivity,"Account want' created",Toast.LENGTH_LONG).show()
                                        hideProgressBar2()
                                    }
                            }
                        } else {
                            val user = User(userName,"", FirebaseAuth.getInstance().currentUser?.uid!!)
                            usersRef.document()
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this@MainActivity,"Account created",Toast.LENGTH_LONG).show()
                                    hideProgressBar2()
                                    sendToAct()
                                }.addOnFailureListener {
                                    Toast.makeText(this@MainActivity,"Account want' created",Toast.LENGTH_LONG).show()
                                    hideProgressBar2()
                                }
                        }
                    }
                } else {
                    Toast.makeText(this, "${task.exception}", Toast.LENGTH_LONG).show()
                    hideProgressBar2()
                }
            }

    }

    private fun showProgressBar1() {
        binding.progressBar1.visibility = View.VISIBLE
    }
    private fun hideProgressBar1() {
        binding.progressBar1.visibility = View.GONE
    }

    private fun showProgressBar2() {
        binding.progressBar2.visibility = View.VISIBLE
    }
    private fun hideProgressBar2() {
        binding.progressBar2.visibility = View.GONE
    }

    private fun startNextAnimation() {
        binding.flipper.setInAnimation(this, android.R.anim.slide_in_left)
        binding.flipper.setOutAnimation(this, android.R.anim.slide_out_right)
        binding.flipper.showNext()
    }

    private fun startPreviousAnimation() {
        binding.flipper.setInAnimation(this, R.anim.slide_in_right)
        binding.flipper.setOutAnimation(this, R.anim.slide_out_left)
        binding.flipper.showPrevious()
    }
    private fun sendToAct() {
        startActivity(Intent(this@MainActivity, ChatActivity::class.java))
        finish()
    }

}