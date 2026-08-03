package com.codeforge.data

import android.util.Base64
import retrofit2.Response

class RepoRepository(private val api: GitHubApi) {
    suspend fun getAuthenticatedUser(): Response<User> = api.getAuthenticatedUser()
    suspend fun listUserRepos(perPage: Int = 100): Response<List<Repo>> = api.listUserRepos(perPage)
    suspend fun getContentsRoot(owner: String, repo: String, branch: String? = null): Response<List<ContentItem>> =
        api.getContentsRoot(owner, repo, branch)
    suspend fun getContents(owner: String, repo: String, path: String, branch: String? = null): Response<List<ContentItem>> =
        api.getContents(owner, repo, path, branch)
    suspend fun getFile(owner: String, repo: String, path: String, ref: String? = null): Response<ContentItem> =
        api.getFile(owner, repo, path, ref)
    suspend fun createOrUpdateFile(owner: String, repo: String, path: String, body: CreateUpdateFileRequest): Response<CreateUpdateFileResponse> =
        api.createOrUpdateFile(owner, repo, path, body)
    suspend fun deleteFile(owner: String, repo: String, path: String, sha: String, message: String, branch: String? = null): Response<CreateUpdateFileResponse> =
        api.deleteFile(owner, repo, path, DeleteFileRequest(message = message, sha = sha, branch = branch))

    suspend fun createFile(owner: String, repo: String, path: String, rawContent: ByteArray, message: String, branch: String? = null): Response<CreateUpdateFileResponse> {
        val encoded = Base64.encodeToString(rawContent, Base64.NO_WRAP)
        return createOrUpdateFile(owner, repo, path, CreateUpdateFileRequest(message = message, content = encoded, branch = branch))
    }

    suspend fun renameFile(owner: String, repo: String, fromPath: String, toPath: String, message: String, branch: String? = null): Pair<Boolean, String?> {
        val fileResp = getFile(owner, repo, fromPath, branch)
        if (!fileResp.isSuccessful) return false to "Source file fetch failed: ${fileResp.code()}"
        val item = fileResp.body() ?: return false to "Source file missing"
        val content = item.content ?: return false to "Source file has no content"
        val bytes = Base64.decode(content.replace("\n", ""), Base64.DEFAULT)
        val createResp = createFile(owner, repo, toPath, bytes, message, branch)
        if (!createResp.isSuccessful) return false to "Create target failed: ${createResp.code()} ${createResp.message()}"
        val sha = item.sha ?: return false to "Source SHA missing"
        val deleteResp = deleteFile(owner, repo, fromPath, sha, "Rename: $message", branch)
        if (!deleteResp.isSuccessful) return false to "Delete source failed: ${deleteResp.code()} ${deleteResp.message()}"
        return true to null
    }

    suspend fun createFolder(owner: String, repo: String, folderPath: String, message: String = "Create folder", branch: String? = null): Response<CreateUpdateFileResponse> {
        val path = folderPath.trimEnd('/') + "/.gitkeep"
        return createFile(owner, repo, path, ByteArray(0), message, branch)
    }

    suspend fun deleteFolderIfEmpty(owner: String, repo: String, folderPath: String, message: String = "Remove folder", branch: String? = null): Pair<Boolean, String?> {
        val resp = getContents(owner, repo, folderPath, branch)
        if (!resp.isSuccessful) return false to "Failed to list folder: ${resp.code()}"
        val items = resp.body().orEmpty()
        if (items.any { it.name != ".gitkeep" }) return false to "Folder not empty"
        val gitkeep = items.firstOrNull { it.name == ".gitkeep" }
        if (gitkeep?.sha != null) {
            val deleteResp = deleteFile(owner, repo, gitkeep.path, gitkeep.sha, message, branch)
            if (!deleteResp.isSuccessful) return false to "Delete .gitkeep failed: ${deleteResp.code()}"
        }
        return true to null
    }

    suspend fun listBranches(owner: String, repo: String): Response<List<Branch>> = api.listBranches(owner, repo)
    suspend fun getBranchRef(owner: String, repo: String, branch: String): Response<GitRef> = api.getBranchRef(owner, repo, branch)
    suspend fun createBranch(owner: String, repo: String, name: String, sha: String): Response<GitRef> =
        api.createBranch(owner, repo, CreateRefRequest(ref = "refs/heads/$name", sha = sha))
    suspend fun listCommits(owner: String, repo: String, branch: String? = null, perPage: Int = 30): Response<List<Commit>> =
        api.listCommits(owner, repo, branch, perPage)
    suspend fun getCommit(owner: String, repo: String, sha: String): Response<CommitWithFiles> = api.getCommit(owner, repo, sha)
    suspend fun searchFiles(owner: String, repo: String, query: String): Response<SearchResponse> =
        api.searchCode("filename:$query repo:$owner/$repo")
    suspend fun searchCode(owner: String, repo: String, query: String): Response<SearchResponse> =
        api.searchCode("$query repo:$owner/$repo")
    suspend fun listPullRequests(owner: String, repo: String, state: String = "open"): Response<List<PullRequest>> =
        api.listPullRequests(owner, repo, state)
    suspend fun createPullRequest(owner: String, repo: String, title: String, body: String, head: String, base: String): Response<PullRequest> =
        api.createPullRequest(owner, repo, CreatePrRequest(title = title, body = body, head = head, base = base))
}
