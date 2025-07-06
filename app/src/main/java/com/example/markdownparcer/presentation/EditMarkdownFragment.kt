package com.example.markdownparcer.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.markdownParcer.R
import com.example.markdownParcer.databinding.FragmentEditMarkdownBinding
import com.example.markdownparcer.domain.parcer.MarkDownParser
import com.example.markdownparcer.presentation.model.ViewState
import kotlinx.coroutines.launch

class EditMarkdownFragment() : Fragment() {

    private var _binding: FragmentEditMarkdownBinding? = null
    private val binding get() = _binding!!

    private val viewModelFactory by lazy {
        MarkdownViewModelFactory(MarkDownParser())
    }
    private val viewModel: MarkdownViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditMarkdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clickListeners()
        stateObserver()
        markdownObserver()
    }


    fun stateObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                when (state) {
                    is ViewState.Error -> {}
                    is ViewState.Idle -> {}
                    is ViewState.Loading -> {}
                    is ViewState.Success -> {}
                }
            }
        }
    }

    fun setStyle(styleMarker: String) {
        val start = binding.editMarkdownTextField.selectionStart
        val end = binding.editMarkdownTextField.selectionEnd
        val text = binding.editMarkdownTextField.text
        if (start == -1 || end == -1) {
            Toast.makeText(
                requireContext(),
                getString(R.string.no_text_selected_warning), Toast.LENGTH_SHORT
            ).show()
        } else {
            val selectedText = text.substring(start, end)
            val styled = selectedText.lines()
                .joinToString("\n") { line ->
                    if (line.isNotBlank()) "**$line**" else line
                }

            text.replace(start, end, "$styleMarker${text.substring(start, end)}$styleMarker")
        }
    }


    fun markdownObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.originalMarkdownFlow.collect { text ->
                if (text != null) {
                    Log.d("MarkdownFragment", "edit markdownObserver: $text")
                    binding.editMarkdownTextField.setText(text)
                }
            }
        }
    }


    fun clickListeners() {
        binding.buttonBold.setOnClickListener {
            setStyle("**")
        }
        binding.buttonItalic.setOnClickListener {
            setStyle("*")
        }
        binding.buttonStrokeThrough.setOnClickListener {
            setStyle("~~")
        }

        binding.saveButton.setOnClickListener {
            viewModel.editMarkdown(binding.editMarkdownTextField.text.toString())
            parentFragmentManager.popBackStack()
        }

        binding.cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}