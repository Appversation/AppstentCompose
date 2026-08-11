package com.appversation.appstentcompose

/** A strict, versioned ViewContent descriptor could not be parsed. */
class ViewContentDescriptorException(
    val descriptorPath: String,
    detail: String,
) : IllegalArgumentException("$descriptorPath: $detail")
