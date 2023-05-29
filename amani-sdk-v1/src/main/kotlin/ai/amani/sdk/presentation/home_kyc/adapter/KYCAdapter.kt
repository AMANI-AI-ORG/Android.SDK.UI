package ai.amani.sdk.presentation.home_kyc.adapter

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.ItemKycBinding
import ai.amani.base.util.SessionManager
import ai.amani.base.utility.AppConstants
import ai.amani.sdk.extentions.getStepConfig
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.show
import ai.amani.sdk.model.customer.Rule
import ai.amani.sdk.presentation.home_kyc.adapter.KYCAdapter.MyViewHolder
import ai.amani.sdk.utils.AppConstant
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import datamanager.model.config.ResGetConfig
import timber.log.Timber


class KYCAdapter(
    dataList: ArrayList<Rule>,
    appConfig: ResGetConfig,
    listener: IKYCListener?,
    context: Context
) : RecyclerView.Adapter<MyViewHolder>() {
    private var appConfig: ResGetConfig? = null
    private val mDocumentList: MutableList<Rule>
    private var layoutInflater: LayoutInflater? = null
    private val listener: IKYCListener?
    private val mContext: Context
    private var flags = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.context)
        }
        val binding: ItemKycBinding = DataBindingUtil.inflate(
            layoutInflater!!, R.layout.item_kyc, parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val stepConfig = appConfig!!.getStepConfig(mDocumentList[position].sortOrder!!)
        val loaderColor = if (appConfig!!.generalConfigs.loaderColor != null)
            appConfig!!.generalConfigs.loaderColor else AppConstants.COLOR_BLACK
        val buttonRadius = if (appConfig!!.generalConfigs.buttonRadiusAndroid != null)
            appConfig!!.generalConfigs.buttonRadiusAndroid else 20

        holder.mBinding.progressBar
            .indeterminateDrawable
            .setColorFilter(Color.parseColor(loaderColor), PorterDuff.Mode.SRC_IN)
        holder.mBinding.progressBar.indeterminateDrawable.setTint(Color.WHITE)
        if (mDocumentList[position].isShowLoader) {
            holder.mBinding.progressBar.show()
        } else {
            holder.mBinding.progressBar.hide()
        }
        Timber.d("Title: ${mDocumentList[position].title}," +
                " Sort Order ${mDocumentList[position].sortOrder}" +
                " Step Id ${mDocumentList[position].id}" +
                " Status : ${mDocumentList[position].status}")

        when (mDocumentList[position].status) {
            AppConstant.STATUS_APPROVED -> {
                holder.mBinding.subTextOfButton.hide()
                holder.mBinding.linearBg.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.custom_btn, null),
                    stepConfig.buttonColor.getApproved(),
                    4,
                    stepConfig.buttonColor.getApproved(),
                    0f,
                    null,
                    true,
                    buttonRadius
                )
                holder.mBinding.textOfButton.setTextProperty(
                    stepConfig.buttonText.getApproved(),
                    stepConfig.getButtonTextColor().getApproved()
                )
            }
            AppConstant.STATUS_AUTOMATICALLY_REJECTED ->                 // If loader is visible
                if (mDocumentList[position].isShowLoader) {
                    holder.mBinding.linearBg.setBackgroundDrawable(
                        ResourcesCompat.getDrawable(
                            mContext.resources,
                            R.drawable.custom_btn,
                            null
                        ), stepConfig.buttonColor.getProcessing(),
                        4, stepConfig.buttonColor.getProcessing(), 0f, null, true, buttonRadius
                    )
                    holder.mBinding.textOfButton.setTextProperty(
                        stepConfig.buttonText.getProcessing(),
                        stepConfig.getButtonTextColor().getProcessing()
                    )
                } else {
                    holder.mBinding.linearBg.setBackgroundDrawable(
                        ResourcesCompat.getDrawable(
                            mContext.resources,
                            R.drawable.custom_btn,
                            null
                        ),
                        stepConfig.buttonColor.getAutomaticallyRejected(),
                        4,
                        stepConfig.buttonColor.getAutomaticallyRejected(),
                        0f,
                        null,
                        true,
                        buttonRadius
                    )
                    holder.mBinding.textOfButton.setTextProperty(
                        stepConfig.buttonText.getAutomaticallyRejected(),
                        stepConfig.getButtonTextColor().getAutomaticallyRejected()
                    )

                    // Sub Title UI: If any error exist, set sub title
                    if (mDocumentList[position].errors != null) {
                        holder.mBinding.subTextOfButton.show()
                        Timber.e("Current document upload response contains errors")
                        if (!mDocumentList[position].errors!!.isEmpty()) {
                            holder.mBinding.subTextOfButton.setTextProperty(
                                mDocumentList[position].errors!![0]!!.errorMessage.toString(),
                                stepConfig.getButtonTextColor().getAutomaticallyRejected()
                            )
                            holder.mBinding.subTextOfButton.setCompoundDrawable(
                                R.drawable.custom_btn,
                                Gravity.TOP,
                                47,
                                true
                            )
                            holder.mBinding.subTextOfButton.visibility = View.VISIBLE
                            holder.mBinding.textOfButton.setCompoundDrawable(
                                R.drawable.custom_btn,
                                Gravity.BOTTOM,
                                47,
                                true
                            )
                        }
                    }
                }
            AppConstant.STATUS_NOT_UPLOADED -> {
                // If loader is visible
                holder.mBinding.subTextOfButton.hide()
                if (mDocumentList[position].isShowLoader) {
                    holder.mBinding.linearBg.setBackgroundDrawable(
                        ResourcesCompat.getDrawable(
                            mContext.resources,
                            R.drawable.custom_btn,
                            null
                        ), stepConfig.buttonColor.getProcessing(),
                        4, stepConfig.buttonColor.getProcessing(), 0f, null, true, buttonRadius
                    )
                    holder.mBinding.textOfButton.setTextProperty(
                        stepConfig.buttonText.getProcessing(),
                        stepConfig.getButtonTextColor().getProcessing()
                    )
                } else {
                    holder.mBinding.linearBg.setBackgroundDrawable(
                        ResourcesCompat.getDrawable(
                            mContext.resources,
                            R.drawable.custom_btn,
                            null
                        ), stepConfig.buttonColor.getNotUploaded(),
                        4, stepConfig.buttonColor.getNotUploaded(), 0f, null, true, buttonRadius
                    )
                    holder.mBinding.textOfButton.setTextProperty(
                        stepConfig.buttonText.getNotUploaded(),
                        stepConfig.getButtonTextColor().getNotUploaded()
                    )
                }
            }
            AppConstant.STATUS_PENDING_REVIEW -> {
                // If loader is visible
                holder.mBinding.subTextOfButton.hide()
                if (mDocumentList[position].isShowLoader) {
                    holder.mBinding.linearBg.setBackgroundDrawable(
                        ResourcesCompat.getDrawable(
                            mContext.resources,
                            R.drawable.custom_btn,
                            null
                        ), stepConfig.buttonColor.getProcessing(),
                        4, stepConfig.buttonColor.getProcessing(), 0f, null, true, buttonRadius
                    )
                    holder.mBinding.textOfButton.setTextProperty(
                        stepConfig.buttonText.getProcessing(),
                        stepConfig.getButtonTextColor().getProcessing()
                    )
                } else {
                    // If status is pending review and loader is not visible
                    holder.mBinding.linearBg.setBackgroundDrawable(
                        ResourcesCompat.getDrawable(
                            mContext.resources,
                            R.drawable.custom_btn,
                            null
                        ), stepConfig.buttonColor.getPendingReview(),
                        4, stepConfig.buttonColor.getPendingReview(), 0f, null, true, buttonRadius
                    )
                    holder.mBinding.textOfButton.setTextProperty(
                        stepConfig.buttonText.getPendingReview(),
                        stepConfig.getButtonTextColor().getPendingReview()
                    )

                    // Sub Title UI: If any error exist, set sub title
                    if (mDocumentList[position].errors!= null) {
                        if (mDocumentList[position].errors!!.isNotEmpty()) {
                            holder.mBinding.subTextOfButton.show()
                            holder.mBinding.subTextOfButton.setTextProperty(
                                mDocumentList[position].errors!![0]!!.errorMessage.toString(),
                                stepConfig.getButtonTextColor().getAutomaticallyRejected()
                            )
                            holder.mBinding.subTextOfButton.setCompoundDrawable(
                                R.drawable.custom_btn,
                                Gravity.TOP,
                                47,
                                true
                            )
                            holder.mBinding.subTextOfButton.visibility = View.VISIBLE
                            holder.mBinding.textOfButton.setCompoundDrawable(
                                R.drawable.custom_btn,
                                Gravity.BOTTOM,
                                47,
                                true
                            )
                        }
                    }
                }
            }
            AppConstant.STATUS_PROCESSING -> {

                holder.mBinding.progressBar.show()
                holder.mBinding.linearBg.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(
                        mContext.resources,
                        R.drawable.custom_btn,
                        null
                    ), stepConfig.buttonColor.getProcessing(),
                    4, stepConfig.buttonColor.getProcessing(), 0f, null, true, buttonRadius
                )
                holder.mBinding.textOfButton.setTextProperty(
                    stepConfig.buttonText.getProcessing(),
                    stepConfig.getButtonTextColor().getProcessing()
                )
            }
            AppConstant.STATUS_REJECTED -> if (mDocumentList[position].isShowLoader) {
                holder.mBinding.linearBg.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.custom_btn, null),
                    stepConfig.buttonColor.getProcessing(),
                    4,
                    stepConfig.buttonColor.getProcessing(),
                    0f,
                    null,
                    true,
                    buttonRadius
                )
                holder.mBinding.textOfButton.setTextProperty(
                    stepConfig.buttonText.getProcessing(),
                    stepConfig.getButtonTextColor().getProcessing()
                )
            } else {
                holder.mBinding.linearBg.setBackgroundDrawable(
                    ResourcesCompat.getDrawable(mContext.resources, R.drawable.custom_btn, null),
                    stepConfig.buttonColor.getRejected(),
                    4,
                    stepConfig.buttonColor.getRejected(),
                    0f,
                    null,
                    true,
                    buttonRadius
                )
                holder.mBinding.textOfButton.setTextProperty(
                    stepConfig.buttonText.getRejected(),
                    stepConfig.getButtonTextColor().getRejected()
                )
                if (mDocumentList[position].errors != null) {
                    if (mDocumentList[position].errors!!.isNotEmpty()) {
                        holder.mBinding.subTextOfButton.show()
                        holder.mBinding.subTextOfButton.setTextProperty(
                            mDocumentList[position].errors!![0]!!.errorMessage.toString(),
                            stepConfig.getButtonTextColor().getAutomaticallyRejected()
                        )
                        holder.mBinding.subTextOfButton.setCompoundDrawable(
                            R.drawable.custom_btn,
                            Gravity.TOP,
                            47,
                            true
                        )
                        holder.mBinding.subTextOfButton.visibility = View.VISIBLE
                        holder.mBinding.textOfButton.setCompoundDrawable(
                            R.drawable.custom_btn,
                            Gravity.BOTTOM,
                            47,
                            true
                        )
                    }
                }
                //break
            }
        }

        // TODO Mandatory steps check will be added here from ViewModel not here
        /*---- Current Button Activation Check----
        * To activate a button there is 2 mandatory check
        * 1) Phase; if phase defined at remote config, Phase 1 is active else is de-active
        * 2) mandatoryStepID; If mandatoryStepID is defined at remote config, check the mandatoryStepID's
        * status to activate current button.
        * For example; There is ButtonA(id = 54, mandatoryStepID = null), ButtonB (id = 55, mandatoryStepID =null),
        * ButtonC(id = 56,mandatoryStepID = 54). In this case, ButtonC cannot be activated before ButtonA status is Approved.
        * When ButtonA status ButtonC will be activated.
        */
        /*     if (stepConfig.getPhase() == 1) {
            if (getFlags() == mDocumentList.size() ) {
                activateButton(holder);
            } else {
                deactivateButton(holder);
            }
        } else {
            int databaseId = 0;
            List<Integer> listOfMandatoryStepID = stepConfig.getMandatoryStepID();
            if (listOfMandatoryStepID == null) {
                activateButton(holder);
            } else {
                StepConfig stepConfig_ = SessionManager.getStepConfig(mContext,position);
                if (stepConfig_ != null) databaseId = stepConfig_.getId();
                if (databaseId != 0) {
                    boolean mandatoryStep = false ;
                    for (int i= 0; i < mDocumentList.size(); i++) {
                        for (int k = 0; k < listOfMandatoryStepID.size(); k ++ ) {
                            if (mDocumentList.get(i).getId().equals(listOfMandatoryStepID.get(k))) {
                                mandatoryStep = mDocumentList.get(i).getStatus().equals(STATUS_APPROVED);
                            }
                        }
                    }
                    if (mandatoryStep) {
                        activateButton(holder);
                    } else {
                        deactivateButton(holder);
                    }
                }
            }
        }*/
    }

    fun setDocumentList(documentList: List<Rule>?) {
        if (!documentList.isNullOrEmpty()) {

            mDocumentList.clear()
            mDocumentList.addAll(documentList)
            notifyDataSetChanged()
        }
    }

    fun updateDocumentList(newDocumentList: List<Rule>?) {
        Timber.i("Document list update is started")
        if (!newDocumentList.isNullOrEmpty()) {
            if (newDocumentList.size == 1) {
                repeat(mDocumentList.size) {
                    if (mDocumentList[it].sortOrder == newDocumentList[0].sortOrder) {
                        mDocumentList[it] = newDocumentList[0]
                        notifyItemChanged(it)
                        Timber.i("Document list index: ${newDocumentList[0].id} is up to date ")
                    }
                }
            } else {
                repeat(newDocumentList.size) {
                    repeat(mDocumentList.size) { index ->
                        if (mDocumentList[index].sortOrder == newDocumentList[it].sortOrder) {
                            mDocumentList[index] = newDocumentList[it]
                            notifyItemChanged(index)
                            Timber.i("Document list index: ${mDocumentList[it].id} is up to date")
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return mDocumentList.size
    }

    interface IKYCListener {
        fun onOnItemSelected(version: Rule)
    }

    inner class MyViewHolder(val mBinding: ItemKycBinding) : RecyclerView.ViewHolder(
        mBinding.root
    ) {
        init {
            mBinding.progressBar.indeterminateDrawable.setColorFilter(
                Color.parseColor(SessionManager.getAppBackground()),
                PorterDuff.Mode.SRC_ATOP
            )
            mBinding.linearBg.setOnClickListener { v: View? ->
                listener?.onOnItemSelected(
                    mDocumentList[adapterPosition]
                )
            }
        }
    }

    /** Activates the current button at holder
     *
     * @param holder: ViewHolder at adapter
     */
    private fun activateButton(holder: MyViewHolder) {
        holder.mBinding.textOfButton.isClickable = true
        holder.mBinding.linearBg.alpha = 1f
    }

    /** De-activates the current button at holder
     *
     * @param holder: ViewHolder at adapter
     */
    private fun deactivateButton(holder: MyViewHolder) {
        holder.mBinding.textOfButton.isClickable = false
        holder.mBinding.linearBg.alpha = 0.5f
    }

    fun getFlags(): Int {
        return flags
    }

    fun setFlags(flags: Int) {
        this.flags = flags
    }

    init {
        this.appConfig = appConfig
        mDocumentList = dataList
        this.listener = listener
        mContext = context
    }
}