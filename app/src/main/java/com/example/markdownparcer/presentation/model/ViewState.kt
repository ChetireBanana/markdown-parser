package com.example.markdownparcer.presentation.model

sealed class ViewState {
    class Idle : ViewState()
    class Loading : ViewState()
    class Error(val message: String) : ViewState()
    class Success(val message: String) : ViewState()
}