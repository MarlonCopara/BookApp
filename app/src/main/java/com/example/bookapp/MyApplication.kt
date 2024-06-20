package com.example.bookapp

import android.app.Application
import android.app.ProgressDialog
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import android.content.Context
import android.widget.Toast
import com.example.bookapp.activities.PdfDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

import java.util.*
import kotlin.collections.HashMap


class MyApplication:Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object{
        fun formatTimeStamp(timestamp: Long):String{
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis=timestamp
            return  DateFormat.format("dd/MM/yyyy",cal).toString()

        }

        fun loadPdfSize(pdfUrl:String,pdfTitle:String,sizeTv:TextView) {
            val TAG = "PDF_SIZE_TAG"

            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.metadata
                .addOnSuccessListener {storageMetaData->
                    Log.d(TAG,"loadPdfSize: got metadata")
                    val bytes= storageMetaData.sizeBytes.toDouble()
                    Log.d(TAG,"loadPdfSize: Size Bytes $bytes")

                    val kb= bytes/1024
                    val mb=kb/1024
                    if(mb>=1){
                        sizeTv.text= "${String.format("%.2f", mb)} MB"
                    }
                    else if(kb>=1){
                        sizeTv.text= "${String.format("%.2f", kb)} KB"

                    }
                    else{
                        sizeTv.text= "${String.format("%.2f", bytes)}bytes"
                    }


                }
                .addOnFailureListener{e->
                    Log.d(TAG,"loadPdfSize: Error al cargar")

                }

        }

        fun  loadPdfFromUrlSinglePage(
            pdfUrl: String,
            pdfTitle: String,
            pdfView: PDFView,
            progressBar: ProgressBar,
            pagesTv: TextView?

        ){

            val TAG="PDF_THUMBNNAIL_TAG"
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener {bytes->

                    Log.d(TAG,"loadPdfSize: Size Bytes $bytes")

                    pdfView.fromBytes(bytes)
                        .pages(0)
                        .spacing(0)
                        .swipeHorizontal(false)
                        .enableSwipe(false)
                        .onError { t->
                            progressBar.visibility= View.INVISIBLE
                            Log.d(TAG,"loadPdfFromUrlSinglePage: ${t.message}")
                        }
                        .onPageError { page, t ->
                            progressBar.visibility=View.INVISIBLE
                            Log.d(TAG,"loadPdfFromUrlSinglePage: ${t.message}")
                        }
                        .onLoad { nbPages->
                            Log.d(TAG, "loadPdfFromUrlSinglePage: Pages: $nbPages")
                            progressBar.visibility=View.INVISIBLE
                            if(pagesTv!=null){
                                pagesTv.text="$nbPages"
                            }
                        }
                        .load()

                }
                .addOnFailureListener{e->
                    Log.d(TAG,"loadPdfSize: Error al cargar")

                }


        }
        fun loadCategory(categoryId:String, categoryTv:TextView){

            val ref =FirebaseDatabase.getInstance().getReference("Categories")
            ref.child(categoryId)
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val category = "${snapshot.child("category").value}"

                        categoryTv.text=category

                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })

        }

        fun deleteBook(context: Context,bookId:String,bookUrl:String, bookTitle:String){
            val TAG="DELETE_BOOK_TAG"

            Log.d(TAG, "deleteBook: Eliminando...")

            val progressDialog= ProgressDialog(context)
            progressDialog.setTitle("Por favor espere...")
            progressDialog.setMessage("Eliminando $bookTitle...")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            Log.d(TAG, "deleteBook: Eliminando del almacenamiento...")
            val storageReference=FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            storageReference.delete()
                .addOnSuccessListener {
                    Log.d(TAG, "deleteBook: Eliminado del almacenamiento")
                    Log.d(TAG, "deleteBook: Eliminando de la base de datos ahora...")

                    val ref= FirebaseDatabase.getInstance().getReference("Books")
                    ref.child(bookId)
                        .removeValue()
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(context,"Se eliminó correctamente... ",Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "deleteBook: Eliminado de la base de datos")


                        }
                        .addOnFailureListener {e->

                            progressDialog.dismiss()
                            Log.d(TAG, "deleteBook: No se pudo eliminar de la base de datos debido a ${e.message}")
                            Toast.makeText(context,"No se pudo eliminar debido a ${e.message}",Toast.LENGTH_SHORT).show()


                        }

                }
                .addOnFailureListener {e->
                    progressDialog.dismiss()
                    Log.d(TAG, "deleteBook: No se pudo eliminar del almacenamiento debido a ${e.message}")
                    Toast.makeText(context,"No se pudo eliminar del almacenamiento debido a ${e.message}",Toast.LENGTH_SHORT).show()

                }


        }

        fun incrementBookViewCount(bookId:String){
            val ref=FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object :ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var viewsCount= "${snapshot.child("viewsCount").value}"

                        if(viewsCount==""||viewsCount=="null"){
                            viewsCount="0";
                        }
                        val newViewsCount=viewsCount.toLong()+1

                        val hashMap=HashMap<String,Any>()
                        hashMap["viewsCount"]=newViewsCount

                        val dbRef=FirebaseDatabase.getInstance().getReference("Books")
                        dbRef.child(bookId)
                            .updateChildren(hashMap)

                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }
        public fun removeFromFavorite(context: Context, bookId: String){
            val TAG="REMOVE_FAV_TAG"
            Log.d(TAG, "removeFromFavorite: Removiendo de favoritos")

            val firebaseAuth=FirebaseAuth.getInstance()

            val ref=FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)

                .removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "removeFromFavorite: Removido de favoritos")
                    Toast.makeText(context,"Removido de favoritos",Toast.LENGTH_SHORT).show()

                }
                .addOnFailureListener {e->
                    Log.d(TAG, "removeFromFavorite: Error al remover de favoritos")
                    Toast.makeText(context,"Error al remover favoritos",Toast.LENGTH_SHORT).show()


                }


        }


  }

}