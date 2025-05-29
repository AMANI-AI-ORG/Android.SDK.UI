package ai.amani.sdk.presentation.otp.questions

import ai.amani.amani_sdk.R
import ai.amani.amani_sdk.databinding.FragmentQuestionnaireBinding
import ai.amani.sdk.extentions.customizeToolBar
import ai.amani.sdk.extentions.hide
import ai.amani.sdk.extentions.hideKeyboard
import ai.amani.sdk.extentions.navigateSafely
import ai.amani.sdk.extentions.show
import ai.amani.sdk.extentions.showSnackbar
import ai.amani.sdk.presentation.common.NavigationCommands
import ai.amani.sdk.presentation.otp.profile_info.ProfileInfoFragmentArgs
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class QuestionnaireFragment : Fragment() {

    private var _binding : FragmentQuestionnaireBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<ProfileInfoFragmentArgs>()
    private val viewModel by viewModels<QuestionnaireViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentQuestionnaireBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setCustomUI()

        viewModel.setArgs(args = args)
        binding.recyclerView.adapter = viewModel.surveyAdapter!!
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        observeLiveEvent()
    }

    private fun observeLiveEvent() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect{
                    when(it){
                        is QuestionnaireState.SubmitButtonClickable -> {
                            binding.submitButton.isClickable = it.clickable
                            if (it.clickable){
                                customButtonUI()
                            } else {
                                customButtonUI(0.5F)
                            }
                        }

                        is QuestionnaireState.Error -> {
                            showSnackbar(it.message)
                            binding.progressBar.hide()
                        }

                        is QuestionnaireState.Loading -> {
                            binding.progressBar.show()
                        }

                        is QuestionnaireState.Success -> {
                            binding.progressBar.hide()
                            binding.submitButton.show()
                        }

                        else -> {

                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            viewModel.navigateTo.collect{
                when(it) {
                   is NavigationCommands.NavigateToHomeScreen -> {
                       viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                           findNavController().clearBackStack(R.id.homeKYCFragment)
                           findNavController().popBackStack(R.id.homeKYCFragment, false)
                       }
                    }

                   is NavigationCommands.NavigateDirections -> {
                       viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                           findNavController().navigateSafely(it.direction)
                       }
                   }
                }
            }
        }

        binding.submitButton.setOnClickListener {
            hideKeyboard()
            viewModel.onClickSubmit()
        }

        binding.recyclerView.addOnScrollListener(object : OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // The recyclerView is not scrolling
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        // The recyclerView is currently being dragged
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            hideKeyboard()
                        }
                    }
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        // The recyclerView is settling (scrolling is finishing)
                    }
                }
            }
        })
    }

    private fun setCustomUI() {
        customizeToolBar(
            args.data.config.topBarBackground,
            args.data.config.topBarFontColor,
            args.data.config.topBarFontColor,
            title= args.data.steps.first().mDocuments?.first()?.versions
                ?.first()?.steps?.first()?.captureTitle?: ""
        )

        customButtonUI(0.5F)
    }

    private fun customButtonUI(opacity: Float = 1.0F) {
        binding.submitButton.setBackgroundDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.custom_btn, null),
            args.data.config.primaryButtonBackgroundColor,
            4, args.data.config.primaryButtonBackgroundColor,
            0f, null,
            true, args.data.config.buttonRadiusAndroid)
        binding.submitButton.alpha = opacity

        binding.submitButton.text = args.data.config.continueText
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}