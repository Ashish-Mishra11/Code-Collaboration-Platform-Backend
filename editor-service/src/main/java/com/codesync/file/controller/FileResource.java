package com.codesync.file.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codesync.file.dto.CodeFileRequest;
import com.codesync.file.dto.CodeFileResponse;
import com.codesync.file.dto.CreateFolderRequest;
import com.codesync.file.dto.MoveFileRequest;
import com.codesync.file.dto.RenameFileRequest;
import com.codesync.file.dto.UpdateContentRequest;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.service.FileService;

import java.util.List;

@Tag(name = "File API", description = "Operations for files and folders")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileResource {

    private final FileService fileService;

    @Operation(summary = "Create a new file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<CodeFileResponse> create(@Valid @RequestBody CodeFileRequest request) {
        CodeFile codeFile = CodeFile.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .path(request.getPath())
                .language(request.getLanguage())
                .content(request.getContent())
                .createdById(request.getCreatedById())
                .lastEditedBy(request.getCreatedById())
                .isDeleted(false)
                .build();

        return ResponseEntity.ok(fileService.createFile(codeFile));
    }

    @Operation(summary = "Create a new folder")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/folder")
    public ResponseEntity<CodeFileResponse> createFolder(@Valid @RequestBody CreateFolderRequest request) {
        return ResponseEntity.ok(
                fileService.createFolder(
                        request.getProjectId(),
                        request.getFolderName(),
                        request.getPath(),
                        request.getCreatedById()
                )
        );
    }

    @Operation(summary = "Get file by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File fetched successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CodeFileResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(fileService.getFileById(id));
    }

    @Operation(summary = "Get all files by project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<CodeFileResponse>> getByProject(@PathVariable Integer projectId) {
        return ResponseEntity.ok(fileService.getFilesByProject(projectId));
    }

    @Operation(summary = "Get file content")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File content fetched successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}/content")
    public ResponseEntity<String> getContent(@PathVariable Integer id) {
        return ResponseEntity.ok(fileService.getFileContent(id));
    }

    @Operation(summary = "Update file content")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File content updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/content")
    public ResponseEntity<CodeFileResponse> updateContent(@PathVariable Integer id,
                                                  @Valid @RequestBody UpdateContentRequest request) {
        return ResponseEntity.ok(
                fileService.updateFileContent(id, request.getContent(), request.getUserId())
        );
    }

    @Operation(summary = "Rename file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File renamed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/rename")
    public ResponseEntity<CodeFileResponse> rename(@PathVariable Integer id,
                                           @Valid @RequestBody RenameFileRequest request) {
        return ResponseEntity.ok(fileService.renameFile(id, request.getNewName()));
    }

    @Operation(summary = "Move file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File moved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/move")
    public ResponseEntity<CodeFileResponse> move(@PathVariable Integer id,
                                         @Valid @RequestBody MoveFileRequest request) {
        return ResponseEntity.ok(fileService.moveFile(id, request.getNewPath()));
    }

    @Operation(summary = "Soft delete file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        fileService.deleteFile(id);
        return ResponseEntity.ok("File soft deleted successfully");
    }

    @Operation(summary = "Restore deleted file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File restored successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/restore")
    public ResponseEntity<CodeFileResponse> restore(@PathVariable Integer id) {
        return ResponseEntity.ok(fileService.restoreFile(id));
    }

    @Operation(summary = "Get file tree")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File tree fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/project/{projectId}/tree")
    public ResponseEntity<List<CodeFileResponse>> getTree(@PathVariable Integer projectId) {
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    @Operation(summary = "Search files in project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search keyword"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/project/{projectId}/search")
    public ResponseEntity<List<CodeFileResponse >> search(@PathVariable Integer projectId,
                                                 @RequestParam String keyword) {
        return ResponseEntity.ok(fileService.searchInProject(projectId, keyword));
    }
}