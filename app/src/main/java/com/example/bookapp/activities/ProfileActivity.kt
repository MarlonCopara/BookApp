package com.example.bookapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.bookapp.MyApplication
import com.example.bookapp.R
import com.example.bookapp.adapters.AdapterPdfFavorite
import com.example.bookapp.databinding.ActivityProfileBinding
import com.example.bookapp.models.ModelPdf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseUser: FirebaseUser

    private lateinit var booksArrayList:ArrayList<ModelPdf>

    private lateinit var adapterPdfFavorite: AdapterPdfFavorite

    private lateinit var  progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.accountTypeTv.text="N/A"
        binding.memberDateTv.text="N/A"
        binding.favoriteBookCountTv.text="N/A"
        binding.accountStatusTv.text="N/A"

        firebaseAuth= FirebaseAuth.getInstance()

        firebaseUser= firebaseAuth.currentUser!!

        progressDialog= ProgressDialog(this)
        progressDialog.setTitle("Por favor espere...")
        progressDialog.setCanceledOnTouchOutside(false)

        loadUserInfo()
        loadFavoriteBooks()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.profileEditBtn.setOnClickListener {
            startActivity(Intent(this,ProfileEditActivity::class.java))

        }

        binding.accountStatusTv.setOnClickListener {
            if(firebaseUser.isEmailVerified){
                Toast.makeText(this,"¡Ya verificado!",Toast.LENGTH_SHORT).show()

            }
            else{
                emailVerificationDialog()

            }
        }


    }

    private fun emailVerificationDialog() {
        val builder=AlertDialog.Builder(this)

        builder.setTitle("Verify Email")
            .setMessage("¿Estás seguro de que quieres enviar instrucciones de verificación por correo electrónico a ${firebaseUser.email}")
            .setPositiveButton("SEND"){d,e->
                sendEmailVerification()

            }
            .setNegativeButton("CANCEL"){d,e->
                d.dismiss()
            }
            .show()

    }

    private fun sendEmailVerification() {
        progressDialog.setMessage("Enviando instrucciones de verificación por correo electrónico a ${firebaseUser.email}")
        progressDialog.show()

        firebaseUser.sendEmailVerification()
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this,"¡Instrucciones enviadas! Verifica tu correo electrónico ${firebaseUser.email}",Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this,"Error al enviar",Toast.LENGTH_SHORT).show()

            }

    }

    private fun loadUserInfo() {

        if(firebaseUser.isEmailVerified){
            binding.accountStatusTv.text="Verified"

        }
        else{
            binding.accountStatusTv.text="Not Verified"

        }
        val ref= FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val email= "${snapshot.child("email").value}"
                    val name= "${snapshot.child("name").value}"
                    val profileImage="${snapshot.child("profileImage").value}"
                    val timestamp="${snapshot.child("timestamp").value}"
                    val uid="${snapshot.child("uid").value}"
                    val userType="${snapshot.child("userType").value}"

                    val formattedDate= MyApplication.formatTimeStamp(timestamp.toLong())

                    binding.nameTv.text=name
                    binding.emailTv.text=email
                    binding.memberDateTv.text=formattedDate
                    binding.accountTypeTv.text=userType

                    try{
                        Glide.with(this@ProfileActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(binding.profileIv)

                    }catch (e: Exception){

                    }


                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadFavoriteBooks(){
        booksArrayList= ArrayList()

        val ref=FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    booksArrayList.clear()
                    for(ds in snapshot.children){
                        val bookId="${ds.child("bookId").value}"

                        val modelPdf=ModelPdf()
                        modelPdf.id=bookId

                        booksArrayList.add(modelPdf)
                    }
                    binding.favoriteBookCountTv.text="${booksArrayList.size}"


                    adapterPdfFavorite= AdapterPdfFavorite(this@ProfileActivity,booksArrayList)
                    binding.favoriteRv.adapter=adapterPdfFavorite
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

    }
}