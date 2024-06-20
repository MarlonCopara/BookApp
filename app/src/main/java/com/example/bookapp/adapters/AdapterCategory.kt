package com.example.bookapp.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bookapp.databinding.RowCategoryBinding
import com.google.firebase.database.FirebaseDatabase
import android.content.Context
import android.content.Intent
import android.widget.Filter
import android.widget.Filterable
import com.example.bookapp.filters.FilterCategory
import com.example.bookapp.models.ModelCategory
import com.example.bookapp.activities.PdfListAdminActivity


class AdapterCategory :RecyclerView.Adapter<AdapterCategory.HolderCategory>,Filterable {
    private val context:Context
    public var categoryArrayList: ArrayList<ModelCategory>
    private var filterList: ArrayList<ModelCategory>

    private var filter: FilterCategory?=null

    private lateinit var binding: RowCategoryBinding

    constructor(context: Context, categoryArrayList: ArrayList<ModelCategory>) {
        this.context = context
        this.categoryArrayList = categoryArrayList
        this.filterList=categoryArrayList
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        binding=RowCategoryBinding.inflate(LayoutInflater.from(context),parent, false)

        return HolderCategory(binding.root)
    }


    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
        val model =categoryArrayList[position]
        val id=model.id
        val category=model.category
        val uid=model.uid
        val timestamp=model.timestamp

        holder.categoryTv.text=category

        holder.deleteBtn.setOnClickListener {
            var builder=AlertDialog.Builder(context)
            builder.setTitle("Eliminar")
                .setMessage("¿Estás seguro de querer eliminar la categoría?")
                .setPositiveButton("Confirmar"){a,d->
                    Toast.makeText(context,"Eliminando ...",Toast.LENGTH_SHORT).show()
                    deleteCategory(model,holder)


                }
                .setNegativeButton("Cancelar"){a,d->
                    a.dismiss()

                }
                .show()
        }

        holder.itemView.setOnClickListener {
            val intent= Intent(context, PdfListAdminActivity::class.java)
            intent.putExtra("categoryId",id)
            intent.putExtra("category",category)
            context.startActivity(intent)
        }


    }

    private fun deleteCategory(model: ModelCategory, holder: HolderCategory) {
        val id=model.id
        val ref=FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context,"Eliminado",Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {e->
                Toast.makeText(context,"No se puede eliminar",Toast.LENGTH_SHORT).show()

            }

    }

    override fun getItemCount(): Int {
        return categoryArrayList.size
    }

    inner class HolderCategory(itemView: View): RecyclerView.ViewHolder(itemView){
        var categoryTv:TextView=binding.categoryTv
        var deleteBtn:ImageButton=binding.deleteBtn

    }

    override fun getFilter(): Filter {
        if(filter==null){
            filter= FilterCategory(filterList,this )

        }
        return filter as FilterCategory
    }


}