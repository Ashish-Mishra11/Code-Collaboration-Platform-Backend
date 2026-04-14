package com.codesync.file.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.codesync.file.dto.CodeFileResponse;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.ResourceNotFoundException;
import com.codesync.file.mapper.CodeFileMapper;
import com.codesync.file.repository.FileRepository;
import com.codesync.file.service.FileService;
import com.codesync.file.util.PathUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;

    @Override
    public CodeFileResponse createFile(CodeFile codeFile) {
        String normalizedPath = PathUtil.normalizePath(codeFile.getPath());

        if (fileRepository.findByProjectIdAndPathAndIsDeletedFalse(codeFile.getProjectId(), normalizedPath).isPresent()) {
            throw new IllegalArgumentException("File already exists at path: " + normalizedPath);
        }

        codeFile.setPath(normalizedPath);
        if (codeFile.getContent() == null) {
            codeFile.setContent("");
        }
        codeFile.setSize((long) codeFile.getContent().getBytes().length);

        CodeFile saved = fileRepository.save(codeFile);
        return CodeFileMapper.toResponse(saved);
    }

    @Override
    public CodeFileResponse getFileById(Integer fileId) {
        CodeFile file = fileRepository.findByFileIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        return CodeFileMapper.toResponse(file);
    }

    @Override
    public List<CodeFileResponse> getFilesByProject(Integer projectId) {
        List<CodeFile> saved =  fileRepository.findByProjectIdAndIsDeletedFalse(projectId);
        return saved.stream()
                .map(CodeFileMapper::toResponse)
                .toList();
    }

    @Override
    public String getFileContent(Integer fileId) {
        CodeFile file = getFileEntityById(fileId);
        return file.getContent();
    }

    @Override
    public CodeFileResponse updateFileContent(Integer fileId, String content, Integer userId) {
        CodeFile file = getFileEntityById(fileId);

        file.setContent(content);
        file.setLastEditedBy(userId);
        file.setSize((long) content.getBytes().length);

        return CodeFileMapper.toResponse(fileRepository.save(file));
    }

    @Override
    public CodeFileResponse renameFile(Integer fileId, String newName) {
        CodeFile file = getFileEntityById(fileId);

        String oldPath = file.getPath();
        String parentPath = PathUtil.getParentPath(oldPath);

        String updatedPath;
        if ("folder".equalsIgnoreCase(file.getLanguage())) {
            updatedPath = PathUtil.buildFolderPath(parentPath, newName);
        } else {
            updatedPath = PathUtil.buildFilePath(parentPath, newName);
        }

        file.setName(newName);
        file.setPath(updatedPath);

        return CodeFileMapper.toResponse(fileRepository.save(file));
    }

    @Override
    public void deleteFile(Integer fileId) {
        CodeFile file = getFileEntityById(fileId);
        file.setIsDeleted(true);
        fileRepository.save(file);
    }

    @Override
    public CodeFileResponse restoreFile(Integer fileId) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        file.setIsDeleted(false);
        return CodeFileMapper.toResponse(fileRepository.save(file));
    }

    @Override
    public CodeFileResponse moveFile(Integer fileId, String newPath) {
        CodeFile file = getFileEntityById(fileId);

        String normalizedPath = PathUtil.normalizePath(newPath);
        file.setPath(normalizedPath);

        return CodeFileMapper.toResponse(fileRepository.save(file));
    }

    @Override
    public CodeFileResponse createFolder(Integer projectId, String folderName, String path, Integer createdById) {
        String folderPath = PathUtil.buildFolderPath(path, folderName);

        if (fileRepository.findByProjectIdAndPathAndIsDeletedFalse(projectId, folderPath).isPresent()) {
            throw new IllegalArgumentException("Folder already exists at path: " + folderPath);
        }

        CodeFile folder = CodeFile.builder()
                .projectId(projectId)
                .name(folderName)
                .path(folderPath)
                .language("folder")
                .content("")
                .size(0L)
                .createdById(createdById)
                .lastEditedBy(createdById)
                .isDeleted(false)
                .build();

        return CodeFileMapper.toResponse(fileRepository.save(folder));
    }

    @Override
    public List<CodeFileResponse> getFileTree(Integer projectId) {
        return fileRepository.findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .sorted((a, b) -> a.getPath().compareToIgnoreCase(b.getPath()))
                .map(CodeFileMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CodeFileResponse> searchInProject(Integer projectId, String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        return fileRepository.findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .filter(file ->
                        (file.getName() != null && file.getName().toLowerCase().contains(lowerKeyword)) ||
                                (file.getPath() != null && file.getPath().toLowerCase().contains(lowerKeyword)) ||
                                (file.getContent() != null && file.getContent().toLowerCase().contains(lowerKeyword))
                )
                .map(CodeFileMapper::toResponse)
                .collect(Collectors.toList());
    }

    private CodeFile getFileEntityById(Integer fileId){
        return fileRepository.findByFileIdAndIsDeletedFalse(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

    }
}