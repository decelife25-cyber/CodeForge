package com.codeforge.data

import retrofit2.Response
import retrofit2.http.*

interface GitHubApi {
    @GET("/user/repos")
    suspend fun listUserRepos(@Query("per_page") perPage: Int = 100): Response<List<Repo>>

    @GET("/repos/{owner}/{repo}/contents")
    suspend fun getContentsRoot(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<ContentItem>>

    @GET("/repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<List<ContentItem>>

    @GET("/repos/{owner}/{repo}/contents/{path}")
    suspend fun getFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): Response<ContentItem>

    @PUT("/repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: CreateUpdateFileRequest
    ): Response<CreateUpdateFileResponse>

    @HTTP(method = "DELETE", path = "/repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: DeleteFileRequest
    ): Response<CreateUpdateFileResponse>
}
