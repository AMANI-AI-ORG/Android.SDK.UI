package ai.amani.sdk.presentation

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.ActivityMainBinding
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.setActionBarColor
import ai.amani.sdk.extentions.show
import ai.amani.sdk.model.KYCResult
import ai.amani.sdk.presentation.home_kyc.CachingHomeKYC
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
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


/**
 * @Author: zekiamani
 * @Date: 1.09.2022
 */
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val viewModel: NFCSharedViewModel by viewModels { NFCSharedViewModel.Factory }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var _binding: ActivityMainBinding? = null
        val binding get() = _binding

        fun setToolBar(
            title: String?,
            navigationIconColor: String?
        ) {
            try {
                title?.let {
                    binding?.toolbarTitle?.text = title
                }
                binding?.toolbar?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun customizeToolBar(
            title: String?,
            titleColor: String?,
            backgroundColor: String?,
            iconColor: String?
        ) {
            try {
                if (title == null
                    || titleColor == null
                    || backgroundColor == null
                    || iconColor == null
                ) return

                binding?.toolbar?.let {
                    it.setBackgroundColor(Color.parseColor(backgroundColor))
                    it.show()
                }

                binding?.toolbarDivider?.dividerColor = Color.parseColor("#CACFD6")

                binding?.toolbarTitle?.let {
                    it.text = title
                    it.setTextColor(Color.parseColor(titleColor))
                }

                iconColor.let {
                    binding?.backButton?.setColorFilter(
                        Color.parseColor(it)
                    )

                    binding?.buttonSelectPdf?.setColorFilter(
                        Color.parseColor(it)
                    )
                }

                binding?.toolbar?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun addSelectButtonListener(onClick: () -> Unit) {
            try {
                binding?.buttonSelectPdf?.let {
                    it.show()
                    it.setOnClickListener {
                        onClick.invoke()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun hideSelectButton() {
            try {
                binding?.buttonSelectPdf?.hide()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun showSelectButton() {
            try {
                binding?.buttonSelectPdf?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun isBackButtonEnabled(isEnabled: Boolean = true) {
            binding?.backButton?.isClickable = isEnabled
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarColor("#000000")

        setTheme(ai.amani.R.style.AppTheme)

        _binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main)


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment?
        navController = navHostFragment!!.navController


        clickListener()

        observeNFCState()

        backPressListener()
    }

    private fun clickListener() {
        binding?.backButton?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val currentFragment = navController.currentDestination?.id
                val previousFragment = navController.previousBackStackEntry?.destination?.id

                if (currentFragment != null) {
                    if (currentFragment == R.id.congratulationsFragment) {
                        finish()
                        return@launch
                    }

                    if (currentFragment == R.id.homeKYCFragment) {
                        finishActivity()
                        return@launch
                    }

                    if (currentFragment == R.id.profileInfoFragment ||
                        currentFragment == R.id.verifyEmailFragment
                        || currentFragment == R.id.questionnaireFragment
                    ) {

                        if (previousFragment == R.id.homeKYCFragment) {
                            finishActivity()
                            return@launch
                        }
                    }
                }

                try {
                    navController.popBackStack()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun backPressListener() {
        this.onBackPressedDispatcher
            .addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
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
                    try {
                        val adapter = NfcAdapter.getDefaultAdapter(this)

                        if (adapter != null) {
                            val intent = Intent(applicationContext, this.javaClass)
                            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                            val pendingIntent =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    PendingIntent.getActivity(
                                        this, 0, Intent(this, javaClass)
                                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), FLAG_MUTABLE
                                    )
                                } else {
                                    PendingIntent.getActivity(
                                        this, 0, Intent(this, javaClass)
                                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
                                    )
                                }
                            val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
                            adapter.enableForegroundDispatch(this, pendingIntent, null, filter)
                        }
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                }

                is NFCActivationState.Disable -> {

                    try {
                        val adapter = NfcAdapter.getDefaultAdapter(this)

                        adapter?.disableForegroundDispatch(this)

                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
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

    override fun onDestroy() {
        super.onDestroy()
        CachingHomeKYC.clearCache()
        _binding = null
    }

}