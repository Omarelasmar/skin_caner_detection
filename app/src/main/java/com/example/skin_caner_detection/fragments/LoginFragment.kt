package com.example.skin_caner_detection.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.skin_caner_detection.R
import com.example.skin_caner_detection.activities.MainActivity
import com.example.skin_caner_detection.databinding.FragmentLoginBinding
import com.example.skin_caner_detection.fragments.dialog.setUpBottomSheetDialog
import com.example.skin_caner_detection.util.NetworkResult
import com.example.skin_caner_detection.viewmodel.LoginViewModel
import com.example.skin_caner_detection.viewmodel.LoginViewModel.Companion.MAIN_ACTIVITY
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val viewModel by viewModels<LoginViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        login()
        observeLogin()
        observeResetPassword()
        binding.signUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.forgotPassword.setOnClickListener {
            setUpBottomSheetDialog {
                viewModel.resetPassword(it)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.navigateState.collect{
                when(it){
                    MAIN_ACTIVITY -> {
                        Intent(requireActivity(), MainActivity::class.java).also { intent ->
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                    else -> {Unit}
                }
            }
        }
    }

    private fun login() {
        binding.apply {
            loginTv.setOnClickListener {
                val email = edEmail.text.toString().trim()
                val password = edPassword.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    viewModel.login(email, password)
                } else {
                    Snackbar.make(
                        binding.root,
                        "Please Enter the Email And Password",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeLogin() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.login.collect {
                    when (it) {
                        is NetworkResult.Loading -> {
                            showProgress()
                        }

                        is NetworkResult.Success -> {
                            Intent(requireActivity(), MainActivity::class.java).also { intent ->
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                            hideProgress()
                            Toast.makeText(requireContext(), "Login Success", Toast.LENGTH_SHORT)
                                .show()
                        }

                        is NetworkResult.Error -> {
                            hideProgress()
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                        }

                        else -> Unit
                    }
                }
            }
        }

    }

    private fun observeResetPassword() {
          lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resetPassword.collectLatest {
                    when (it) {
                        is NetworkResult.Loading -> {
                            //do nothing
                        }

                        is NetworkResult.Success -> {
                            Snackbar.make(
                                binding.root,
                                "Password reset email sent",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }

                        is NetworkResult.Error -> {
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
    private fun hideProgress() {
        binding.progressBar2.visibility = View.GONE
    }

    private fun showProgress() {
        binding.progressBar2.visibility = View.VISIBLE
    }
}
