package com.example.markdownparcer.presentation

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.markdownParcer.R
import com.example.markdownParcer.databinding.FragmentLoadingTextBinding
import com.example.markdownparcer.domain.model.MarkDownElement
import com.example.markdownparcer.domain.model.ParsedMarkDown
import com.example.markdownparcer.domain.parcer.MarkDownParser
import com.example.markdownparcer.presentation.model.ViewState
import kotlinx.coroutines.launch

class LoadingMarkdownFragment() : Fragment() {

    private var _binding: FragmentLoadingTextBinding? = null
    private val binding get() = _binding!!

    private val viewModelFactory by lazy {
        MarkdownViewModelFactory(MarkDownParser())
    }
    private val viewModel: MarkdownViewModel by activityViewModels { viewModelFactory }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = requireContext().contentResolver.openInputStream(it)
            val text = inputStream?.bufferedReader().use { reader -> reader?.readText() } ?: ""
            viewModel.parse(text)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoadingTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stateListener()
        markdownListener()
        clickListeners()

    }

    private fun clickListeners() {
        binding.enterUrlEditText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s != null) {
                        binding.loadMarkdownFromUrl.isEnabled = s.isNotEmpty()
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                }
            }
        )
        binding.loadMarkdownFromMemory.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("text/markdown", "text/plain"))
        }
        binding.loadMarkdownFromUrl.setOnClickListener {
            val url = binding.enterUrlEditText.text.toString().trim()
            viewModel.loadMarkdownFromUrl(url)
        }

        binding.openFullTextButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, ReadMarkdownFragment())
                .addToBackStack(null)
                .commit()
        }
    }


    private fun markdownListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.parsedMarkdownFlow.collect { text ->
                if (text != null) {
                    showPreview(text)
                }
            }
        }
    }

    private fun stateListener() {
        viewLifecycleOwner.lifecycleScope.launch {

            viewModel.viewState.collect { state ->
                when (state) {
                    is ViewState.Idle -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.previewTextField.visibility = View.VISIBLE
                        binding.previewTextTitle.visibility = View.GONE
                        binding.openFullTextButton.visibility = View.GONE
                        binding.loadMarkdownFromUrl.isEnabled = false
                        binding.loadMarkdownFromMemory.isEnabled = true
                    }

                    is ViewState.Loading -> {
                        binding.loadingIndicator.visibility = View.VISIBLE
                        binding.previewTextField.visibility = View.GONE
                        binding.previewTextTitle.visibility = View.GONE
                        binding.openFullTextButton.visibility = View.GONE
                        binding.loadMarkdownFromUrl.isEnabled = false
                        binding.loadMarkdownFromMemory.isEnabled = false
                    }

                    is ViewState.Error -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.previewTextField.visibility = View.VISIBLE
                        binding.previewTextTitle.visibility = View.GONE
                        binding.openFullTextButton.visibility = View.GONE
                        binding.loadMarkdownFromUrl.isEnabled = true
                        binding.loadMarkdownFromMemory.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                    is ViewState.Success -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.previewTextField.visibility = View.VISIBLE
                        binding.previewTextTitle.visibility = View.VISIBLE
                        binding.openFullTextButton.visibility = View.VISIBLE
                        binding.loadMarkdownFromMemory.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }


    private fun showPreview(parsedMarkDown: ParsedMarkDown) {
        val builder = StringBuilder()

        parsedMarkDown.content.take(10).forEach { element ->
            when (element) {
                is MarkDownElement.Header -> {
                    builder.append("${element.text}\n\n")
                }

                is MarkDownElement.Paragraph -> {
                    val paragraph = element.textPart.joinToString("")
                    builder.append("$paragraph\n\n")
                }

                is MarkDownElement.Image -> {
                    builder.append("[${element.description}]\n\n")
                }

                is MarkDownElement.Table -> {
                    builder.append("[${element.table[0]}]\n\n")
                }
            }

        }
        val size = parsedMarkDown.content.size

        builder.append("............Ещё ${size - 10} строк...>\n\n")

        binding.previewTextField.text = builder.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}