package ai.amani.sdk.presentation.select_document_type.adapter

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.ItemSelectDocumentFragmentBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import datamanager.model.config.GeneralConfigs
import datamanager.model.config.Version

class DocumentAdapter(
    private var mDocumentList: List<Version?>?,
    private val listener: IDocumentListener?,
    private var generalConfig: GeneralConfigs?
) : RecyclerView.Adapter<DocumentAdapter.MyViewHolder?>() {
    private var layoutInflater: LayoutInflater? = null


    inner class MyViewHolder(val binding: ItemSelectDocumentFragmentBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        init {
            binding.btnDocument.setOnClickListener { v: View? ->
                listener?.onOnItemSelected(
                    mDocumentList!![adapterPosition]!! // TODO null check
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext())
        }
        val binding: ItemSelectDocumentFragmentBinding = DataBindingUtil.inflate(
            layoutInflater!!,
            R.layout.item_select_document_fragment,
            parent,
            false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val buttonRadius = if (generalConfig!!.buttonRadiusAndroid != null
        ) generalConfig!!.buttonRadiusAndroid else 20

        holder.binding.btnDocument.setBackgroundDrawable(
            ResourcesCompat.getDrawable(
                holder.binding.btnDocument.context.resources,
                R.drawable.custom_btn, null
            ),
            generalConfig!!.getPrimaryButtonBackgroundColor(),
            4,
            if (generalConfig!!.getPrimaryButtonBorderColor() != null
            ) generalConfig!!.getPrimaryButtonBorderColor() else generalConfig!!.getPrimaryButtonBackgroundColor(),
            0F,
            null,
            true,
            buttonRadius
        )
        holder.binding.btnDocument.setTextProperty(
            mDocumentList!![position]!!.title,
            generalConfig!!.getPrimaryButtonTextColor()
        )
    }

    override fun getItemCount(): Int {
        return mDocumentList!!.size
    }

    interface IDocumentListener {
        fun onOnItemSelected(version: Version?)
    }
}