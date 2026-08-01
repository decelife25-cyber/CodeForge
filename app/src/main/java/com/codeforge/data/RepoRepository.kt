package com.codeforge.data

import retrofit2.Response

class RepoRepository(private val api: GitHubApi) {

    suspend fun listUserRepos(perPage: Int = 100): Response<List<Repo>> = api.listUserRepos(perPage)

    suspend fun getContentsRoot(owner: String, repo: String): Response<List<ContentItem>> =
        api.getContentsRoot(owner, repo)

    suspend fun getContents(owner: String, repo: String, path: String): Response<List<ContentItem>> =
        api.getContents(owner, repo, path)

    suspend fun getFile(owner: String, repo: String, path: String, ref: String? = null): Response<ContentItem> =
        api.getFile(owner, repo, path, ref)

    suspend fun createOrUpdateFile(owner: String, repo: String, path: String, body: CreateUpdateFileRequest): Response<CreateUpdateFileResponse> =
        api.createOrUpdateFile(owner, repo, path, body)
}
