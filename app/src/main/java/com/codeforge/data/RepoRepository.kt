package com.codeforge.data

import retrofit2.Response
import android.util.Base64

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

    suspend fun deleteFile(owner: String, repo: String, path: String, sha: String, message: String): Response<CreateUpdateFileResponse> {
        val body = DeleteFileRequest(message = message, sha = sha)
        return api.deleteFile(owner, repo, path, body)
    }

    suspend fun createFile(owner: String, repo: String, path: String, rawContent: ByteArray, message: String): Response<CreateUpdateFileResponse> {
        val encoded = Base64.encodeToString(rawContent, Base64.NO_WRAP)
        val req = CreateUpdateFileRequest(message = message, content = encoded)
        return createOrUpdateFile(owner, repo, path, req)
    }

    suspend fun renameFile(owner: String, repo: String, fromPath: String, toPath: String, message: String): Pair<Boolean, String?> {
        // Fetch existing file
        val fileResp = getFile(owner, repo, fromPath)
        if (!fileResp.isSuccessful) return Pair(false, "Source file fetch failed: ${fileResp.code()}")
        val item = fileResp.body()
        if (item == null || item.content == null) return Pair(false, "Source file has no content")

        // Clean base64 content (remove newlines)
        val cleaned = item.content.replace("\n", "")
        val bytes = Base64.decode(cleaned, Base64.DEFAULT)

        // Create new file
        val createResp = createFile(owner, repo, toPath, bytes, message)
        if (!createResp.isSuccessful) return Pair(false, "Create target failed: ${createResp.code()} ${createResp.message()}")

        // Delete old file using its sha
        val sha = item.sha ?: return Pair(false, "Source SHA missing")
        val delResp = deleteFile(owner, repo, fromPath, sha, "Rename: $message")
        if (!delResp.isSuccessful) return Pair(false, "Delete source failed: ${delResp.code()} ${delResp.message()}")

        return Pair(true, null)
    }

    suspend fun createFolder(owner: String, repo: String, folderPath: String, message: String = "Create folder") : Response<CreateUpdateFileResponse> {
        // GitHub doesn't have real folders; create a .gitkeep file inside
        val path = if (folderPath.endsWith("/")) folderPath + ".gitkeep" else folderPath + "/.gitkeep"
        val encoded = Base64.encodeToString(ByteArray(0), Base64.NO_WRAP)
        val req = CreateUpdateFileRequest(message = message, content = encoded)
        return createOrUpdateFile(owner, repo, path, req)
    }

    suspend fun deleteFolderIfEmpty(owner: String, repo: String, folderPath: String, message: String = "Remove folder") : Pair<Boolean, String?> {
        // List contents of the folder
        val resp = getContents(owner, repo, folderPath)
        if (!resp.isSuccessful) return Pair(false, "Failed to list folder: ${resp.code()}")
        val items = resp.body() ?: emptyList()
        // If empty or only contains .gitkeep, try to remove .gitkeep
        val nonGitkeep = items.filter { it.name != ".gitkeep" }
        if (nonGitkeep.isNotEmpty()) return Pair(false, "Folder not empty")

        // Find .gitkeep
        val gitkeep = items.find { it.name == ".gitkeep" }
        if (gitkeep != null && gitkeep.sha != null) {
            val del = deleteFile(owner, repo, gitkeep.path, gitkeep.sha, message)
            return if (del.isSuccessful) Pair(true, null) else Pair(false, "Delete .gitkeep failed: ${del.code()}")
        }

        // Nothing to delete; consider success
        return Pair(true, null)
    }
}
