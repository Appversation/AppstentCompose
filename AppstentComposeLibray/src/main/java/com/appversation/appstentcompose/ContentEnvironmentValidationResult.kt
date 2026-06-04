package com.appversation.appstentcompose

data class ContentEnvironmentValidationResult(
    val contentEnvironment: String,
    val isValid: Boolean,
    val statusCode: Int?,
    val message: String?
)
