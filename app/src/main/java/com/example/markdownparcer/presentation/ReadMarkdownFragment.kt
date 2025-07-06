package com.example.markdownparcer.presentation

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.markdownParcer.R
import com.example.markdownParcer.databinding.FragmentReadMarkdownBinding
import com.example.markdownparcer.domain.model.MarkDownElement
import com.example.markdownparcer.domain.model.ParsedMarkDown
import com.example.markdownparcer.domain.model.TableElement
import com.example.markdownparcer.domain.model.TextPart
import com.example.markdownparcer.domain.parcer.MarkDownParser
import com.example.markdownparcer.presentation.model.ViewState
import kotlinx.coroutines.launch

class ReadMarkdownFragment() : Fragment() {

    private var _binding: FragmentReadMarkdownBinding? = null
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
        _binding = FragmentReadMarkdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadImagesBitmaps()
        markdownObserver()
        stateObserver()
        binding.editTextButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, EditMarkdownFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun markdownObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.parsedMarkdownFlow.collect { text ->
                if (text != null) {
                    showMarkdown(text)
                }
            }
        }
    }


    private fun stateObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewState.collect { state ->
                when (state) {
                    is ViewState.Idle -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.markdownContainer.visibility = View.VISIBLE
                        binding.editTextButton.isEnabled = false
                    }

                    is ViewState.Loading -> {
                        binding.loadingIndicator.visibility = View.VISIBLE
                        binding.markdownContainer.visibility = View.GONE
                        binding.editTextButton.isEnabled = false
                    }

                    is ViewState.Error -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.markdownContainer.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        binding.editTextButton.isEnabled = false
                    }

                    is ViewState.Success -> {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.markdownContainer.visibility = View.VISIBLE
                        binding.editTextButton.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    private fun showMarkdown(markdown: ParsedMarkDown) {
        binding.markdownContainer.removeAllViews()
        markdown.content.forEach { element ->
            when (element) {
                is MarkDownElement.Header -> showHeader(element)
                is MarkDownElement.Paragraph -> showParagraph(element)
                is MarkDownElement.Image -> showImage(element)
                is MarkDownElement.Table -> showTable(element)
            }
        }
    }

    private fun showHeader(header: MarkDownElement.Header) {
        val textView = TextView(requireContext()).apply {
            text = header.text
            when (header.level) {
                1 -> {
                    textSize = 24f
                    setTypeface(typeface, Typeface.BOLD)
                }

                2 -> {
                    textSize = 20f
                    setTypeface(typeface, Typeface.BOLD)
                }

                3 -> {
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                }

                else -> {
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                }
            }
            setPadding(0, 8, 0, 8)
        }
        binding.markdownContainer.addView(textView)
    }

    private fun showParagraph(paragraph: MarkDownElement.Paragraph) {
        val textView = TextView(requireContext()).apply {
            text = buildStyledText(paragraph.textPart)
            setPadding(0, 4, 0, 4)
        }
        binding.markdownContainer.addView(textView)
    }

    private fun showImage(image: MarkDownElement.Image) {
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            image.bitmap?.let {
                setImageBitmap(it)
            } ?: run {
                setImageResource(R.drawable.outline_imagesmode_24)
                contentDescription = image.description
            }
            setPadding(0, 8, 0, 8)
        }
        binding.markdownContainer.addView(imageView)
    }

    private fun showTable(table: MarkDownElement.Table) {
        val tableLayout = TableLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
            isStretchAllColumns = true
        }
        val rows = mutableListOf<List<String>>()

        for (elements in table.table) {
            when (elements) {
                is TableElement.TableHeader -> {
                    rows.add(elements.cells)
                }

                is TableElement.TableRow -> {
                    rows.add(elements.cells)
                }

                is TableElement.TableHeaderDivider -> {}
            }
        }
        for (row in rows) {
            val tableRow = TableRow(requireContext())
            for (cellText in row) {
                val textView = TextView(requireContext()).apply {
                    setBackgroundResource(R.drawable.cell_borders)
                    text = cellText
                    setPadding(8, 4, 8, 4)
                }
                tableRow.addView(textView)
            }
            tableLayout.addView(tableRow)
        }
        binding.markdownContainer.addView(tableLayout)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun buildStyledText(parts: List<TextPart>): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        parts.forEach { part ->
            when (part) {
                is TextPart.SimpleText -> {
                    builder.append(part.text)
                }

                is TextPart.BoldText -> {
                    val start = builder.length
                    builder.append(part.text)
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                is TextPart.ItalicText -> {
                    val start = builder.length
                    builder.append(part.text)
                    builder.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        start,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                is TextPart.StrikeThroughText -> {
                    val start = builder.length
                    builder.append(part.text)
                    builder.setSpan(
                        StrikethroughSpan(),
                        start,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                is TextPart.InlineImage -> {
                    val bitmap = part.bitmap
                    val drawable = bitmap?.toDrawable(requireContext().resources)?.apply {
                        val maxHeight = 64
                        val maxWidth = 64
                        val aspectRatio = bitmap.width.toFloat() / bitmap.height
                        val width =
                            if (aspectRatio >= 1f) maxWidth else (maxHeight * aspectRatio).toInt()
                        val height =
                            if (aspectRatio < 1f) maxHeight else (maxWidth / aspectRatio).toInt()
                        setBounds(0, 0, width, height)
                    }
                        ?: requireContext().getDrawable(R.drawable.outline_imagesmode_24)!!.apply {
                            setBounds(0, 0, 50, 50)
                        }

                    val start = builder.length
                    builder.append(" ")
                    builder.setSpan(
                        ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
                        start,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        return builder
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}