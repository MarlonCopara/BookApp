package com.example.bookapp.activities

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.example.bookapp.databinding.ActivityPdfDetailBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.Adapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bookapp.Constants
import com.example.bookapp.MyApplication
import com.example.bookapp.R
import com.example.bookapp.adapters.AdapterComment
import com.example.bookapp.databinding.DialogCommentAddBinding
import com.example.bookapp.models.ModelComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream
import java.lang.Exception

class PdfDetailActivity : AppCompatActivity() {

    private lateinit var binding:ActivityPdfDetailBinding

    private companion object{
        const val TAG="BOOK_DETAILS_TAG"
    }

    private var bookId=""
    private var bookTitle=""
    private var bookUrl=""

    private var isInMyFavorite=false

    private lateinit var firebaseAuth:FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    private lateinit var commentArrayList:ArrayList<ModelComment>

    private lateinit var adapterComment:AdapterComment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId=intent.getStringExtra("bookId")!!

        progressDialog=ProgressDialog(this)
        progressDialog.setTitle("Por favor espere...")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth=FirebaseAuth.getInstance()

        if(firebaseAuth.currentUser!=null){
            checkIsFavorite()
        }

        MyApplication.incrementBookViewCount(bookId)

        loadBookDetails()

        showComments()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.readBootBtn.setOnClickListener {
            val intent=Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId",bookId);
            startActivity(intent)
        }

        binding.downloadBookBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onCreate: STORAGE PERMISSION is already granted")
                downloadBook()
            }
            else{
                Log.d(TAG, "onCreate: STORAGE PERMISSION was not granted, LETS request it")
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)


            }


        }
        binding.favoriteBtn.setOnClickListener {
            if(firebaseAuth.currentUser==null){
                Toast.makeText(this,"No has iniciado sesión", Toast.LENGTH_SHORT).show()
            }
            else{
                if(isInMyFavorite){

                    MyApplication.removeFromFavorite(this,bookId)

                }
                else{
                    addToFavorite()

                }

            }

        }
        binding.addCommentBtn.setOnClickListener {
            if(firebaseAuth.currentUser == null){
                Toast.makeText(this,"No has iniciado sesión",Toast.LENGTH_SHORT).show()
            }
            else{
                addCommentDialog()
            }
        }


    }

    private fun showComments() {
        commentArrayList= ArrayList()

        val ref=FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comentario")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    commentArrayList.clear()
                    for(ds in snapshot.children){
                        val model =ds.getValue(ModelComment::class.java)

                        commentArrayList.add(model!!)
                    }
                    adapterComment= AdapterComment(this@PdfDetailActivity,commentArrayList)

                    binding.commentsRv.adapter=adapterComment

                }


                override fun onCancelled(error: DatabaseError) {

                }
            })

    }

    private var comment=""

    private fun addCommentDialog() {
        val commentAddBinding=DialogCommentAddBinding.inflate(LayoutInflater.from(this))

        val builder=AlertDialog.Builder(this,R.style.CustomDialog)
        builder.setView(commentAddBinding.root)

        val alertDialog=builder.create()
        alertDialog.show()

        commentAddBinding.backBtn.setOnClickListener { alertDialog.dismiss() }

        commentAddBinding.submitBtn.setOnClickListener {
            comment = commentAddBinding.commentEt.text.toString().trim()

            if(comment.isEmpty()){
                Toast.makeText(this,"Ingrese el comentario",Toast.LENGTH_SHORT).show()
            }
            else{
                alertDialog.dismiss()
                addComment()
            }
        }

    }

    private fun addComment() {
        progressDialog.setMessage("Agregando comentario")
        progressDialog.show()

        val timestamp="${System.currentTimeMillis()}"

        val hashMap= HashMap<String, Any>()
        hashMap["id"]="$timestamp"
        hashMap["bookId"]="$bookId"
        hashMap["timestamp"]="$timestamp"
        hashMap["comment"]="$comment"
        hashMap["uid"]="${firebaseAuth.uid}"

        val ref= FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comentario").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this,"Comentario Agregado",Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this,"Error al agregar el comentario",Toast.LENGTH_SHORT).show()

            }
    }

    private val requestStoragePermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean->
        if(isGranted){
            Log.d(TAG, "onCreate: STORAGE PERMISSION is granted")
            downloadBook()

        }
        else{
            Log.d(TAG, "onCreate: STORAGE PERMISSION is denied")
            Toast.makeText(this,"Permiso denegado",Toast.LENGTH_SHORT).show()

        }


    }

    private fun downloadBook(){

        Log.d(TAG, "downloadBook: Descargando Libro")

        progressDialog.setMessage("Descargando Libro")
        progressDialog.show()

        val storageReference=FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener {bytes->
                Log.d(TAG, "downloadBook: Libro descargado")
                saveToDownloadsFolder(bytes)


            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Log.d(TAG, "downloadBook: No se pudo descargar el libro")
                Toast.makeText(this,"No se pudo descargar el libro",Toast.LENGTH_SHORT).show()

            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray?) {
        Log.d(TAG, "saveToDownloadsFolder: guardando libro descargado")

        val nameWithExtention="${System.currentTimeMillis()}.pdf"

        try {
            val downloadsFolder=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs()

            val filePath=downloadsFolder.path+"/"+nameWithExtention

            val out= FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this,"Guardado en la carpeta de Descargas",Toast.LENGTH_SHORT).show()
            Log.d(TAG, "saveToDownloadsFolder: Guardado en la carpeta de Descargas")

            progressDialog.dismiss()

            incrementDownloadCount()



        }catch (e: Exception){
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownloadsFolder: No se pudo guardar")
            Toast.makeText(this,"No se pudo guardar",Toast.LENGTH_SHORT).show()

        }


    }

    private fun incrementDownloadCount() {
        Log.d(TAG, "incrementDownloadCount: ")

        val ref= FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    var downloadsCount="${snapshot.child("downloadsCount").value}"
                    Log.d(TAG, "onDataChange: Recuento de descargas actuales: $downloadsCount")

                    if(downloadsCount ==""||downloadsCount =="null"){
                        downloadsCount = "0"
                    }
                    val newDownloadCount: Long= downloadsCount.toLong()+1
                    Log.d(TAG, "onDataChange: Recuento de nuevas descargas: $newDownloadCount")

                    val hashMap: HashMap<String,Any> = HashMap()
                    hashMap["downloadsCount"]=newDownloadCount

                    val dfRef=FirebaseDatabase.getInstance().getReference("Books")
                    dfRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: Contador de descargas incrementado")

                        }
                        .addOnFailureListener {e->
                            Log.d(TAG, "onDataChange: Error al incrementar")

                        }



                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

    }

    private fun loadBookDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categoryId= "${snapshot.child("categoryId").value}"
                    val description= "${snapshot.child("description").value}"
                    val downloadsCount= "${snapshot.child("downloadsCount").value}"
                    val timestamp= "${snapshot.child("timestamp").value}"
                    bookTitle= "${snapshot.child("title").value}"
                    val uid= "${snapshot.child("uid").value}"
                    bookUrl= "${snapshot.child("url").value}"
                    val viewsCount= "${snapshot.child("viewsCount").value}"

                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    MyApplication.loadPdfFromUrlSinglePage(
                        "$bookUrl",
                        "$bookTitle",
                        binding.pdfView,
                        binding.progressBar,
                        binding.pagesTv
                    )

                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)

                    binding.titleTv.text=bookTitle
                    binding.descriptionTv.text=description
                    binding.viewsTv.text=viewsCount
                    binding.downloadsTv.text=downloadsCount
                    binding.dateTv.text=date

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
    private fun checkIsFavorite(){
        Log.d(TAG, "checkIsFavorite: Verificando si el libro está en favoritos o no")

        val ref=FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite=snapshot.exists()
                    if(isInMyFavorite){
                        Log.d(TAG, "onDataChange: Disponible en favoritos")
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_filled_white,0,0)
                        binding.favoriteBtn.text="Remover Favorito"

                    }
                    else{
                        Log.d(TAG, "onDataChange: No disponible en favoritos")

                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_border_white,0,0)
                        binding.favoriteBtn.text="Favorito"

                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

    }

    private fun addToFavorite(){
        Log.d(TAG, "addToFavorite: Agregando a favoritos")
        val timestamp = System.currentTimeMillis()

        val hashMap=HashMap<String, Any>()
        hashMap["bookId"]=bookId
        hashMap["timestamp"]=timestamp

        val ref=FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "addToFavorite: Agregando a favoritos")
                Toast.makeText(this,"Agregando a favoritos",Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {e->
                Log.d(TAG, "addToFavorite: Error al agregar a favoritos")
                Toast.makeText(this,"Error al agregar a favoritos",Toast.LENGTH_SHORT).show()
                
            }

    }


}