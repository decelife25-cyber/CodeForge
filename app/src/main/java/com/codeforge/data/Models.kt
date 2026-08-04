package com.codeforge.data

import com.squareup.moshi.Json
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
data class Owner(
    val login: String,
    val avatar_url: String? = null
)

@JsonClass(generateAdapter = true)
data class User(
    val login: String,
    val avatar_url: String
)

@JsonClass(generateAdapter = true)
data class ContentItem(
    val name: String,
    val path: String,
    val sha: String? = null,
    val type: String,
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
    val content: ContentItem? = null,
    val commit: Commit? = null
)

@JsonClass(generateAdapter = true)
data class DeleteFileRequest(
    val message: String,
    val sha: String,
    val branch: String? = null
)

@JsonClass(generateAdapter = true)
data class Branch(
    val name: String,
    val commit: BranchCommit
)

@JsonClass(generateAdapter = true)
data class BranchCommit(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class GitRef(
    val ref: String,
    @Json(name = "object") val objectInfo: GitObject
)

@JsonClass(generateAdapter = true)
data class GitObject(
    val sha: String,
    val type: String
)

@JsonClass(generateAdapter = true)
data class CreateRefRequest(
    val ref: String,
    val sha: String
)

@JsonClass(generateAdapter = true)
data class Commit(
    val sha: String,
    val commit: CommitDetail,
    val parents: List<CommitParent> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CommitDetail(
    val message: String,
    val author: CommitAuthor
)

@JsonClass(generateAdapter = true)
data class CommitAuthor(
    val name: String,
    val date: String
)

@JsonClass(generateAdapter = true)
data class CommitParent(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class CommitFile(
    val filename: String,
    val patch: String? = null,
    val status: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0
)

@JsonClass(generateAdapter = true)
data class CommitWithFiles(
    val sha: String,
    val commit: CommitDetail,
    val files: List<CommitFile> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val total_count: Int,
    val items: List<SearchItem>
)

@JsonClass(generateAdapter = true)
data class SearchItem(
    val name: String,
    val path: String,
    val repository: SearchRepo
)

@JsonClass(generateAdapter = true)
data class SearchRepo(
    val full_name: String
)

@JsonClass(generateAdapter = true)
data class PullRequest(
    val number: Int,
    val title: String,
    val body: String? = null,
    val state: String,
    val head: PrRef,
    val base: PrRef,
    val html_url: String,
    val user: Owner
)

@JsonClass(generateAdapter = true)
data class PrRef(
    val ref: String,
    val sha: String
)

@JsonClass(generateAdapter = true)
data class CreatePrRequest(
    val title: String,
    val body: String,
    val head: String,
    val base: String
)
