package com.codesync.file;

import com.codesync.file.dto.CodeFileResponse;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.ResourceNotFoundException;
import com.codesync.file.repository.FileRepository;
import com.codesync.file.service.impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EditorService (FileService) Unit Tests")
class EditorServiceApplicationTests {

    @Mock private FileRepository fileRepository;

    @InjectMocks
    private FileServiceImpl fileService;

    private CodeFile sampleFile;

    @BeforeEach
    void setUp() {
        sampleFile = new CodeFile();
        sampleFile.setFileId(1);
        sampleFile.setProjectId(10);
        sampleFile.setName("Main.java");
        sampleFile.setPath("/Main.java");
        sampleFile.setContent("public class Main {}");
        sampleFile.setIsDeleted(false);
        
        sampleFile.setSize(20L);
    }

    // ── createFile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createFile: saves and returns file when path is unique")
    void createFile_success() {
        when(fileRepository.findByProjectIdAndPathAndIsDeletedFalse(10, "Main.java"))
                .thenReturn(Optional.empty());
        when(fileRepository.save(any())).thenReturn(sampleFile);

        CodeFileResponse resp = fileService.createFile(sampleFile);

        assertThat(resp).isNotNull();
        verify(fileRepository).save(sampleFile);
    }

    @Test
    @DisplayName("createFile: throws when file already exists at path")
    void createFile_duplicatePath() {
        when(fileRepository.findByProjectIdAndPathAndIsDeletedFalse(10, "Main.java"))
                .thenReturn(Optional.of(sampleFile));

        assertThatThrownBy(() -> fileService.createFile(sampleFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createFile: defaults content to empty string when null")
    void createFile_nullContentDefaultsEmpty() {
        sampleFile.setContent(null);
        sampleFile.setPath("/empty.js");
        when(fileRepository.findByProjectIdAndPathAndIsDeletedFalse(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(fileRepository.save(any())).thenAnswer(inv -> {
            CodeFile f = inv.getArgument(0);
            assertThat(f.getContent()).isEqualTo("");
            return f;
        });

        fileService.createFile(sampleFile);
        verify(fileRepository).save(any());
    }

    // ── getFileById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFileById: returns response when file exists")
    void getFileById_found() {
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleFile));

        CodeFileResponse resp = fileService.getFileById(1);

        assertThat(resp).isNotNull();
        assertThat(resp.getFileId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getFileById: throws ResourceNotFoundException when not found")
    void getFileById_notFound() {
        when(fileRepository.findByFileIdAndIsDeletedFalse(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getFileById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found");
    }

    // ── getFilesByProject ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getFilesByProject: returns all non-deleted files for project")
    void getFilesByProject() {
        CodeFile f2 = new CodeFile();
        f2.setFileId(2); f2.setProjectId(10); f2.setName("Utils.java");
        f2.setPath("/Utils.java"); f2.setContent(""); f2.setIsDeleted(false);  f2.setSize(0L);

        when(fileRepository.findByProjectIdAndIsDeletedFalse(10)).thenReturn(List.of(sampleFile, f2));

        List<CodeFileResponse> result = fileService.getFilesByProject(10);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getFilesByProject: returns empty list when project has no files")
    void getFilesByProject_empty() {
        when(fileRepository.findByProjectIdAndIsDeletedFalse(99)).thenReturn(List.of());
        assertThat(fileService.getFilesByProject(99)).isEmpty();
    }

    // ── getFileContent ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFileContent: returns raw content string")
    void getFileContent() {
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleFile));

        String content = fileService.getFileContent(1);

        assertThat(content).isEqualTo("public class Main {}");
    }

    // ── updateFileContent ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateFileContent: updates content, size, and lastEditedBy")
    void updateFileContent_success() {
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleFile));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CodeFileResponse resp = fileService.updateFileContent(1, "public class Main { int x; }", 5);

        assertThat(resp.getContent()).isEqualTo("public class Main { int x; }");
        verify(fileRepository).save(argThat(f ->
                f.getLastEditedBy().equals(5) && f.getContent().equals("public class Main { int x; }")));
    }

    @Test
    @DisplayName("updateFileContent: throws when file not found")
    void updateFileContent_notFound() {
        when(fileRepository.findByFileIdAndIsDeletedFalse(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.updateFileContent(99, "code", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteFile (soft delete) ───────────────────────────────────────────────

    @Test
    @DisplayName("deleteFile: sets isDeleted=true (soft delete)")
    void deleteFile_softDelete() {
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleFile));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fileService.deleteFile(1);

        verify(fileRepository).save(argThat(f -> Boolean.TRUE.equals(f.getIsDeleted())));
    }
}
