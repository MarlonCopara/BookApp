package com.example.bookapp.activities

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.bookapp.R
import com.example.bookapp.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding:ActivityForgotPasswordBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog:ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth=FirebaseAuth.getInstance()

        progressDialog= ProgressDialog(this)
        progressDialog.setTitle("Espere por favor...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private var email = ""

    private fun validateData() {
        email=binding.emailEt.text.toString().trim()

        if(email.isEmpty()){
            Toast.makeText(this,"Ingrese el correo",Toast.LENGTH_SHORT).show()
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this,"Correo electrónico inválido",Toast.LENGTH_SHORT).show()

        }
        else{
            recoverPassword()
        }

    }

    private fun recoverPassword() {
        progressDialog.setMessage("Enviando instrucciones para restablecer la contraseña a $email")
        progressDialog.show()

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this,"Instrucciones enviadas a \n$email",Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this,"No se puedo enviar a \n$email",Toast.LENGTH_SHORT).show()


            }

    }
}