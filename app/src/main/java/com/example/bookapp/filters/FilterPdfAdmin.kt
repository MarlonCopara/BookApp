package com.example.bookapp.filters

import android.widget.Filter
import com.example.bookapp.adapters.AdapterPdfAdmin
import com.example.bookapp.models.ModelPdf

class FilterPdfAdmin: Filter {

    var filterlist:ArrayList<ModelPdf>
    var adapterPdfAdmin: AdapterPdfAdmin

    constructor(filterlist: ArrayList<ModelPdf>, adapterPdfAdmin: AdapterPdfAdmin) {
        this.filterlist = filterlist
        this.adapterPdfAdmin = adapterPdfAdmin
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint:CharSequence?=constraint
        val results=FilterResults()
        if (constraint!=null && constraint.isEmpty()){
            constraint=constraint.toString().lowercase()
            var filteredModels=ArrayList<ModelPdf>()
            for(i in filterlist.indices){

                if(filterlist[i].title.lowercase().contains(constraint)){
                    filteredModels.add(filterlist[i])
                }
            }
            results.count=filteredModels.size
            results.values=filteredModels

        }
        else{
            results.count=filterlist.size
            results.values=filterlist
        }
        return  results

    }

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        adapterPdfAdmin.pdfArrayList= results.values as ArrayList<ModelPdf>
        adapterPdfAdmin.notifyDataSetChanged()

    }

}