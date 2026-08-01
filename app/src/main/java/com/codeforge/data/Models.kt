package com.codeforge.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Repo(
    val id: Long,
    val name: String,
    val full_name: String,
    val private: Boolean,
    val owner: Owner
)

@JsonClass(generateAdapter = true)
data class Owner(val login: String)

@JsonClass(generateAdapter = true)
data class ContentItem(
    val name: String,
    val path: String,
    val sha: String?,
    val type: String, // "file" or "dir"
    val encoding: String? = null,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateUpdateFileRequest(
    val message: String,
    val content: String,
    val branch: String? = null,
    val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateUpdateFileResponse(
    val content: ContentItem?,
    val commit: Any?
)

// Request body for deleting a file
@JsonClass(generateAdapter = true)
data class DeleteFileRequest(
    val message: String,
    val sha: String,
    val branch: String? = null
)
