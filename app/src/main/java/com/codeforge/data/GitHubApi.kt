package com.codeforge.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApi {
    @GET("/user")
    suspend fun getAuthenticatedUser(): Response<User>

    @GET("/user/repos")
    suspend fun listUserRepos(@Query("per_page") perPage: Int = 100): Response<List<Repo>>

    @GET("/repos/{owner}/{repo}/contents")
    suspend fun getContentsRoot(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") ref: String? = null
    ): Response<List<ContentItem>>

    @GET("/repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") ref: String? = null
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

    @GET("/repos/{owner}/{repo}/branches")
    suspend fun listBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<Branch>>

    @GET("/repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun getBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "branch", encoded = true) branch: String
    ): Response<GitRef>

    @POST("/repos/{owner}/{repo}/git/refs")
    suspend fun createBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateRefRequest
    ): Response<GitRef>

    @GET("/repos/{owner}/{repo}/commits")
    suspend fun listCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") branch: String? = null,
        @Query("per_page") perPage: Int = 30
    ): Response<List<Commit>>

    @GET("/repos/{owner}/{repo}/commits/{sha}")
    suspend fun getCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): Response<CommitWithFiles>

    @GET("/search/code")
    suspend fun searchCode(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 30
    ): Response<SearchResponse>

    @GET("/repos/{owner}/{repo}/pulls")
    suspend fun listPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open"
    ): Response<List<PullRequest>>

    @POST("/repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreatePrRequest
    ): Response<PullRequest>
}
