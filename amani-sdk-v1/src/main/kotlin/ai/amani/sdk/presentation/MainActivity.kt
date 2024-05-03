package ai.amani.sdk.presentation

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.ActivityMainBinding
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.setActionBarColor
import ai.amani.sdk.extentions.setColorFilter
import ai.amani.sdk.extentions.show
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.presentation.common.OnBackPressedDispatcher.backPressed
import ai.amani.sdk.presentation.nfc.NFCActivationState
import ai.amani.sdk.presentation.nfc.NFCSharedViewModel
import ai.amani.sdk.utils.AppConstant
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.Intent
import android.graphics.Color
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController


/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
class MainActivity: AppCompatActivity() {

    private lateinit var navController: NavController
    private val viewModel: NFCSharedViewModel by viewModels{NFCSharedViewModel.Factory}

    companion object {
       @SuppressLint("StaticFieldLeak")
       lateinit var binding: ActivityMainBinding

       fun setToolBar(
           title: String?,
           navigationIconColor: String?
       ) {
           title?.let {
               binding.toolbarTitle.text = title
           }
           binding.toolbar.show()
       }

       fun customizeToolBar(
           title: String?,
           titleColor: String?,
           backgroundColor: String?,
           iconColor: String?
       ) {
           if (title == null
               || titleColor == null
               || backgroundColor == null
               || iconColor == null) return

           binding.toolbar.let {
               it.setBackgroundColor(Color.parseColor(backgroundColor))
               it.show()
           }

           binding.toolbarDivider.dividerColor = Color.parseColor("#CACFD6")

           binding.toolbarTitle.let {
               it.text = title
               it.setTextColor(Color.parseColor(titleColor))
           }

           iconColor.let {
               binding.backButton.setColorFilter(
                   Color.parseColor(it)
               )

               binding.buttonSelectPdf.setColorFilter(
                   Color.parseColor(it)
               )
           }

           binding.toolbar.show()
       }

       fun addSelectButtonListener(onClick: () -> Unit) {
           binding.buttonSelectPdf.let {
               it.show()
               it.setOnClickListener {
                   onClick.invoke()
               }
           }
       }

       fun hideSelectButton(exception : (e: Exception) -> Unit) {
           try {
               binding.buttonSelectPdf.hide()
           } catch (e: Exception) {
               exception.invoke(e)
           }
       }

       fun isBackButtonEnabled(isEnabled: Boolean = true) {
           binding.backButton.isClickable = isEnabled
       }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarColor("#000000")

        setTheme(ai.amani.R.style.AppTheme)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment?
        navController = navHostFragment!!.navController

        binding.backButton.setOnClickListener {
            val currentFragment = navController.currentDestination?.id
            val previousFragment = navController.previousBackStackEntry?.destination?.id

            if (currentFragment != null) {
                if (currentFragment == R.id.congratulationsFragment) {
                    finish()
                    return@setOnClickListener
                }

                if (currentFragment == R.id.homeKYCFragment) {
                    finishActivity()
                    return@setOnClickListener
                }

                if (currentFragment == R.id.profileInfoFragment || currentFragment == R.id.verifyEmailFragment
                    || currentFragment == R.id.questionnaireFragment  ) {

                    if (previousFragment == R.id.homeKYCFragment) {
                        finishActivity()
                        return@setOnClickListener
                    }
                }
            }

            navController.popBackStack()
        }

        observeNFCState()

        this.onBackPressedDispatcher
            .addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressed()
                    if (navController.currentDestination?.id!! == R.id.homeKYCFragment) finishActivity()
                    else this@MainActivity.onBackPressedDispatcher.onBackPressed()
                }
            })
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val currentFragment = navController.currentDestination!!.id

        if (currentFragment == R.id.NFCScanFragment) {
            viewModel.set(intent)
        }
    }

    private fun observeNFCState() {
        viewModel.nfcActivationState.observe(this) {

            when (it) {
                is NFCActivationState.Enable -> {
                    val adapter = NfcAdapter.getDefaultAdapter(this)

                    if (adapter != null) {
                        val intent = Intent(applicationContext, this.javaClass)
                        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PendingIntent.getActivity(this, 0, Intent(this, javaClass)
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), FLAG_MUTABLE)
                        } else{
                            PendingIntent.getActivity(this, 0, Intent(this, javaClass)
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
                        }
                        val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
                        adapter.enableForegroundDispatch(this, pendingIntent, null, filter)
                    }
                }

                is NFCActivationState.Disable -> {

                    val adapter = NfcAdapter.getDefaultAdapter(this)

                    adapter?.let {
                        it.disableForegroundDispatch(this)
                    }
                }

                else -> {}
            }
        }
    }


    /**
     * Finish whole activity and return the relevant data to client
     */
    private fun finishActivity() {
        //Finishing activity with KYCResult as INCOMPLETE
        val intent = Intent()
        intent.putExtra(AppConstant.KYC_RESULT, KYCResult())
        this.setResult(Activity.RESULT_OK, intent)
        finish()
    }


}