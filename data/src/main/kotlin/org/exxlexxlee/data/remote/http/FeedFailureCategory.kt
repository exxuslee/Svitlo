package org.exxlexxlee.data.remote.http

import kotlinx.serialization.SerializationException
import java.io.IOException

enum class FeedFailureCategory { NETWORK, SCHEMA, REMOTE }

fun Exception.feedFailureCategory(): FeedFailureCategory = when (this) {
    is IOException -> FeedFailureCategory.NETWORK
    is SerializationException, is IllegalArgumentException, is NoSuchElementException,
    is ClassCastException -> FeedFailureCategory.SCHEMA
    else -> FeedFailureCategory.REMOTE
}
