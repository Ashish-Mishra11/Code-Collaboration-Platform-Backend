package com.codesync.file.service;


import java.util.List;

import com.codesync.file.dto.CodeFileResponse;
import com.codesync.file.entity.CodeFile;

public interface FileService {

	CodeFileResponse createFile(CodeFile codeFile);

	CodeFileResponse getFileById(Integer fileId);

	List<CodeFileResponse> getFilesByProject(Integer projectId);

	String getFileContent(Integer fileId);

	CodeFileResponse updateFileContent(Integer fileId, String content, Integer userId);

	CodeFileResponse renameFile(Integer fileId, String newName);

	void deleteFile(Integer fileId);

	CodeFileResponse restoreFile(Integer fileId);

	CodeFileResponse moveFile(Integer fileId, String newPath);

	CodeFileResponse createFolder(Integer projectId, String folderName, String path, Integer createdById);

	List<CodeFileResponse> getFileTree(Integer projectId);

	List<CodeFileResponse> searchInProject(Integer projectId, String keyword);
}