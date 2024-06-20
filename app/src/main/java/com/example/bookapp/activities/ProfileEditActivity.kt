package com.example.bookapp.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bookapp.R
import com.example.bookapp.databinding.ActivityProfileEditBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Por favor espere...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        loadUserInfo()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.profileIv.setOnClickListener {
            showImageAttachMenu()
        }

        binding.updateBtn.setOnClickListener {
            validateData()
        }
    }

    private var name = ""

    private fun validateData() {
        name = binding.nameEt.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Ingrese el nombre", Toast.LENGTH_SHORT).show()
        } else {
            if (imageUri == null) {
                updateProfile("")
            } else {
                uploadImage()
            }
        }
    }

    private fun uploadImage() {
        progressDialog.setMessage("Cargando imagen del perfil")
        progressDialog.show()

        val filePathAndName = "ProfileImages/${firebaseAuth.uid}_${System.currentTimeMillis()}"

        val reference = FirebaseStorage.getInstance().getReference(filePathAndName)
        reference.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                reference.downloadUrl.addOnSuccessListener { uri ->
                    val uploadedImageUrl = uri.toString()
                    updateProfile(uploadedImageUrl)
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile(uploadedImageUrl: String) {
        progressDialog.setMessage("Actualizando perfil...")

        val hashmap: HashMap<String, Any> = HashMap()
        hashmap["name"] = name
        if (imageUri != null) {
            hashmap["profileImage"] = uploadedImageUrl
        }

        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(firebaseAuth.uid!!)
            .updateChildren(hashmap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()

                // Después de actualizar el perfil, verifica el rol del usuario
                checkUserRole()

            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error al actualizar el perfil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserRole() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(firebaseAuth.uid!!)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.child("role").getValue(String::class.java)
                if (role == "user") {
                    // Si el rol es "user", inicia DashboardUserActivity
                    startActivity(Intent(this@ProfileEditActivity, DashboardUserActivity::class.java))
                } else if (role == "admin") {
                    // Si el rol es "admin", inicia DashboardAdminActivity
                    startActivity(Intent(this@ProfileEditActivity, DashboardAdminActivity::class.java))
                }
                // Puedes agregar lógica adicional para otros roles si es necesario
            }

            override fun onCancelled(error: DatabaseError) {
                // Maneja onCancelled
            }
        })
    }

    private fun loadUserInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"

                    binding.nameEt.setText(name)

                    try {
                        Glide.with(this@ProfileEditActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(binding.profileIv)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled
                }
            })
    }

    private fun showImageAttachMenu() {
        val popupMenu = PopupMenu(this, binding.profileIv)
        popupMenu.menu.add(Menu.NONE, 0, 0, "Cámara")
        popupMenu.menu.add(Menu.NONE, 1, 1, "Galería")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> pickImageCamera()
                1 -> pickImageGallery()
            }
            true
        }
    }

    private fun pickImageCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp_Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                binding.profileIv.setImageURI(imageUri)
            } else {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageUri = data?.data
                binding.profileIv.setImageURI(imageUri)
            } else {
                Toast.makeText(this, "Cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
