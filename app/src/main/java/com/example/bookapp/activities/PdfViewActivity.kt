package com.example.bookapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.bookapp.Constants
import com.example.bookapp.databinding.ActivityPdfViewBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class PdfViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewBinding

    private companion object{
        const val TAG="PDF_VIEW_TAG"
    }

    var bookId=""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPdfViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId=intent.getStringExtra("bookId")!!
        loadBooksDetails()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }



    private fun loadBooksDetails() {
        Log.d(TAG, "loadBooksDetails: Obtener URL del PDF desde la base de datos")

        val ref=FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pdfUrl=snapshot.child(  "url").value
                    Log.d(TAG, "onDataChange: PDF_URL: $pdfUrl")

                    loadBookFromUrl("$pdfUrl")

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })


    }

    private fun loadBookFromUrl(pdfUrl: String) {
        Log.d(TAG, "loadBookFromUrl: Obtener PDF desde el almacenamiento de Firebase utilizando la URL")
        val reference=FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        reference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "loadBookFromUrl: PDF obtenido desde la URL")
                binding.pdfView.fromBytes(bytes)
                    .swipeHorizontal(false)
                    .onPageChange { page, pageCount ->
                        val currentPage = page + 1
                        binding.toolbarSubtitleTv.text = "$currentPage/$pageCount"
                        Log.d(TAG, "loadBookFromUrl: $currentPage/$pageCount")

                    }
                    .onError { t ->
                        Log.d(TAG, "loadBookFromUrl: ${t.message}")

                    }
                    .onPageError { page, t ->
                        Log.d(TAG, "loadBookFromUrl: ${t.message}")

                    }
                    .load()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener {e->
                Log.d(TAG, "loadBookFromUrl: Error al obtener el PDF")
                binding.progressBar.visibility = View.GONE

            }

    }
}