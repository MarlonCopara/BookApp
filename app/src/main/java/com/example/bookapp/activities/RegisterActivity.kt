package com.example.bookapp.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.bookapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth=FirebaseAuth.getInstance()

        progressDialog= ProgressDialog(this)
        progressDialog.setTitle("Por favor espere...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener{
            onBackPressed()
        }

        binding.registerBtn.setOnClickListener {
            validateData()
        }


    }

    private var name=""
    private var email=""
    private var password=""

    private fun validateData() {
        name=binding.nameEt.text.toString().trim()
        email=binding.emailEt.text.toString().trim()
        password=binding.passwordEt.text.toString().trim()
        val cPassword=binding.cPasswordEt.text.toString().trim()

        if(name.isEmpty()){
            Toast.makeText(this,"Ingrese su nombre",Toast.LENGTH_SHORT).show()
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this,"Patrón de Correo Invalido",Toast.LENGTH_SHORT).show()
        }
        else if(password.isEmpty()){
            Toast.makeText(this,"Ingrese la contraseña.",Toast.LENGTH_SHORT).show()

        }
        else if(cPassword.isEmpty()){
            Toast.makeText(this,"Confirme la contraseña",Toast.LENGTH_SHORT).show()

        }
        else if (password != cPassword){
            Toast.makeText(this,"La contraseña no coincide",Toast.LENGTH_SHORT).show()
        }
        else{
            createUserAccount()
        }





    }

    private fun createUserAccount() {
        progressDialog.setMessage("Creando Cuenta...")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(email,password)
            .addOnSuccessListener {

                updateUserInfo()

            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this,"Error al crear la cuenta",Toast.LENGTH_SHORT).show()

            }


    }

    private fun updateUserInfo() {
        progressDialog.setMessage("Guardando la información del usuario")

        val timestamp=System.currentTimeMillis()

        val uid=firebaseAuth.uid

        val hashMap: HashMap<String,Any?> = HashMap()
        hashMap["uid"]=uid
        hashMap["email"]=email
        hashMap["name"]=name
        hashMap["profileImage"]=""
        hashMap["userType"]="user"
        hashMap["timestamp"]=timestamp

        val ref=FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this,"Cuenta Creada",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, DashboardUserActivity::class.java))
                finish()

            }
            .addOnFailureListener{e->
                progressDialog.dismiss()
                Toast.makeText(this,"Error al guardar la información",Toast.LENGTH_SHORT).show()

            }



    }
}