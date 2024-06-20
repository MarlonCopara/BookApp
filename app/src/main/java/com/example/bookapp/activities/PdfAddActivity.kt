package com.example.bookapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bookapp.databinding.ActivityPdfAddBinding
import com.example.bookapp.models.ModelCategory
import com.google.android.gms.tasks.Task

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

import kotlin.collections.ArrayList

class PdfAddActivity : AppCompatActivity() {

    private  lateinit var binding: ActivityPdfAddBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    private lateinit var categoryArrayList: ArrayList<ModelCategory>

    private var pdfUri : Uri? = null

    private val TAG="PDF_ADD_TAG"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPdfAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth=FirebaseAuth.getInstance()
        loadPdfCategories()

        progressDialog= ProgressDialog(this)
        progressDialog.setTitle("Por favor espere...")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.categoryTv.setOnClickListener {
            categoryPickDialog()
        }
        binding.attachPdfBtn.setOnClickListener {
            pdfPickIntent()
        }
        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private var title=""
    private var description=""
    private var category=""

    private fun validateData() {
        Log.d(TAG, "validateData: Validando datos")

        title=binding.titleEt.text.toString().trim()
        description=binding.descriptionEt.text.toString().trim()
        category=binding.categoryTv.text.toString().trim()

        if (title.isEmpty()){
            Toast.makeText(this,"Ingrese el título",Toast.LENGTH_SHORT).show()
        }
        else if(description.isEmpty()){
            Toast.makeText(this,"Ingrese la descripción",Toast.LENGTH_SHORT).show()

        }
        else if(category.isEmpty()){
            Toast.makeText(this,"Escoja la categoría",Toast.LENGTH_SHORT).show()

        }
        else if(pdfUri==null){
            Toast.makeText(this,"Escoja el PDF",Toast.LENGTH_SHORT).show()

        }
        else{
            uploadPdfToStorage()

        }

    }

    private fun uploadPdfToStorage() {
        Log.d(TAG, "uploadPdfToStorage: Cargando al almacenamiento")

        progressDialog.setMessage("Subiendo PDF")
        progressDialog.show()

        val timestamp=System.currentTimeMillis()

        val filePathAndName="Books/$timestamp"

        val storageReference= FirebaseStorage.getInstance().getReference(filePathAndName)
        storageReference.putFile(pdfUri!!)
            .addOnSuccessListener {taskSnapshot->
                Log.d(TAG, "uploadPdfToStorage: PDF subido, ahora obteniendo url...")

                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedPdfUrl="${uriTask.result}"

                uploadPdfInfoToDb(uploadedPdfUrl,timestamp)



            }
            .addOnFailureListener{e->
                Log.d(TAG, "uploadPdfToStorage: Error al subir")
                progressDialog.dismiss()
                Toast.makeText(this,"Error al subir",Toast.LENGTH_SHORT).show()


            }

    }

    private fun uploadPdfInfoToDb(uploadedPdfUrl: String, timestamp: Long) {
        Log.d(TAG, "uploadPdfInfoToDb: Subiendo a la base de datos")
        progressDialog.setMessage("Subiendo información del pdf...")

        val uid=firebaseAuth.uid

        val hashMap:HashMap<String,Any> = HashMap()
        hashMap["uid"]="$uid"
        hashMap["id"]="$timestamp"
        hashMap["title"]="$title"
        hashMap["description"]="$description"
        hashMap["categoryId"]="$selectedCategoryId"
        hashMap["url"]="$uploadedPdfUrl"
        hashMap["timestamp"]=timestamp
        hashMap["viewsCount"]=0
        hashMap["downloadsCount"]=0


        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "uploadPdfInfoToDb: Cargado en la base de datos")
                progressDialog.dismiss()
                Toast.makeText(this,"Cargado",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@PdfAddActivity, DashboardAdminActivity::class.java))
                pdfUri=null

            }
            .addOnFailureListener {e->
                Log.d(TAG, "uploadPdfInfoToDb: Error al subir")
                progressDialog.dismiss()
                Toast.makeText(this,"Error al subir",Toast.LENGTH_SHORT).show()

            }



    }

    private fun loadPdfCategories() {
        Log.d(TAG,"loadPdfCategories: Cargando categorías")
        categoryArrayList= ArrayList()

        val ref=FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryArrayList.clear()
                for(ds in snapshot.children){
                    val model=ds.getValue(ModelCategory::class.java)

                    categoryArrayList.add(model!!)
                    Log.d(TAG,"onDataChange: ${model.category}")
                }


            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private var selectedCategoryId=""
    private var selectedCategoryTitle=""
    private fun categoryPickDialog(){
        Log.d(TAG,"categoryPickDialog: Mostrando diálogo de selección de categoría de PDF")

        val categoriesArray= arrayOfNulls<String>(categoryArrayList.size)
        for (i in categoryArrayList.indices){
            categoriesArray[i]=categoryArrayList[i].category
        }
        val builder= AlertDialog.Builder(this)
        builder.setTitle("Seleccionar Categoría")
            .setItems(categoriesArray){dialog,which->

                selectedCategoryTitle=categoryArrayList[which].category
                selectedCategoryId=categoryArrayList[which].id

                binding.categoryTv.text=selectedCategoryTitle

                Log.d(TAG,"categoryPickDialog:  Categoría seleccionada ID: ${selectedCategoryId}")
                Log.d(TAG,"categoryPickDialog: Título seleccionado ID: ${selectedCategoryTitle}")

            }
            .show()
    }
    private fun pdfPickIntent(){
        Log.d(TAG, "pdfPickIntent: Iniciando intent para seleccionar PDF")

        val intent=Intent()
        intent.type="application/pdf"
        intent.action=Intent.ACTION_GET_CONTENT
        pdfActivityResultLauncher.launch(intent)

    }

    val pdfActivityResultLauncher=registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback <ActivityResult>{ result ->
            if (result.resultCode== RESULT_OK){
                Log.d(TAG, "PDF Picked: ")
                pdfUri=result.data!!.data
            }
            else{
                Log.d(TAG, "PDF Pick cancelled: ")
                Toast.makeText(this,"Cancelado",Toast.LENGTH_SHORT).show()
            }
        }
    )



}