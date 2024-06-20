package com.example.bookapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.bookapp.databinding.ActivityPdfEditBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PdfEditActivity : AppCompatActivity() {

    private lateinit var binding:ActivityPdfEditBinding

    private companion object{
        private const val TAG="PDF_EDIT_TAG"
    }

    private var bookId=""

    private lateinit var progressDialog:ProgressDialog

    private lateinit var categoryTitleArrayList:ArrayList<String>

    private lateinit var categoryIdArrayList: ArrayList<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPdfEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId=intent.getStringExtra("bookId")!!

        progressDialog= ProgressDialog(this)
        progressDialog.setTitle("Por favor espere...")
        progressDialog.setCanceledOnTouchOutside(false)

        loadCategories()
        loadBookInfo()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.categoryTv.setOnClickListener {
            categoryDialog()

        }
        binding.submitBtn.setOnClickListener {
            validateData()

        }

    }

    private fun loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Cargando información del libro")

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {
                    selectedCategoryId=snapshot.child("categoryId").value.toString()
                    val description =snapshot.child("description").value.toString()
                    val title=snapshot.child("title").value.toString()

                    binding.titleEt.setText(title)
                    binding.descriptionEt.setText(description)

                    Log.d(TAG, "onDataChange: Cargando información de la categoría")
                    val refBookCategory=FirebaseDatabase.getInstance().getReference("Categories")
                    refBookCategory.child(selectedCategoryId)
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val category=snapshot.child("category").value
                                binding.categoryTv.text=category.toString()

                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private var title=""
    private var description=""

    private fun validateData() {
        title=binding.titleEt.text.toString().trim()
        description=binding.descriptionEt.text.toString().trim()

        if(title.isEmpty()){
            Toast.makeText(this,"Introduce el título",Toast.LENGTH_SHORT).show()


        }
        else if(description.isEmpty()){
            Toast.makeText(this,"Introduce la descripción",Toast.LENGTH_SHORT).show()

        }
        else if(selectedCategoryId.isEmpty()){
            Toast.makeText(this,"Elije la categoría",Toast.LENGTH_SHORT).show()
        }
        else{
            updatePdf()
        }


    }

    private fun updatePdf() {
        Log.d(TAG, "updatePdf: Comenzando a actualizar la información del pdf...")

        progressDialog.setMessage("Actualizando la información del libro...")
        progressDialog.show()

        val hashMap=HashMap<String,Any>()
        hashMap["title"]="$title"
        hashMap["description"]="$description"
        hashMap["categoryId"]="$selectedCategoryId"

        val ref=FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Log.d(TAG, "updatePdf: Se actualizó correctamente...")
                Toast.makeText(this,"Se actualizó correctamente...",Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@PdfEditActivity, DashboardAdminActivity::class.java))
                finish()

            }
            .addOnFailureListener {e->
                Log.d(TAG, "updatePdf: Error al actualizar")
                progressDialog.dismiss()
                Toast.makeText(this,"Error al actualizar",Toast.LENGTH_SHORT).show()

            }


    }

    private var selectedCategoryId=""
    private var selectedCategoryTitle=""

    private fun categoryDialog() {
        val categoriesArray= arrayOfNulls<String>(categoryTitleArrayList.size)
        for (i in categoryTitleArrayList.indices){
            categoriesArray[i]=categoryTitleArrayList[i]
        }

        var builder=AlertDialog.Builder(this)
        builder.setTitle("Elija la categoría")
            .setItems(categoriesArray){dialog,position->
                selectedCategoryId= categoryIdArrayList[position]
                selectedCategoryTitle=categoryTitleArrayList[position]

                binding.categoryTv.text=selectedCategoryTitle

            }
            .show()

    }

    private fun loadCategories() {
        Log.d(TAG, "loadCategories: Cargando categorías...")
        categoryTitleArrayList= ArrayList()
        categoryIdArrayList= ArrayList()

        val ref= FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryIdArrayList.clear()
                categoryTitleArrayList.clear()

                for(ds in snapshot.children){
                    val id = "${ds.child("id").value}"
                    val category = "${ds.child("category").value}"

                    categoryIdArrayList.add(id)
                    categoryTitleArrayList.add(category)

                    Log.d(TAG, "onDataChange: Categoria Id $id")
                    Log.d(TAG, "onDataChange: Categoria $category")
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

    }
}