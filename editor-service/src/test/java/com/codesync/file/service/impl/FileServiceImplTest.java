package com.codesync.file.service.impl;

import com.codesync.file.dto.CodeFileResponse;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.ResourceNotFoundException;
import com.codesync.file.feignclient.ProjectServiceClient;
import com.codesync.file.repository.FileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Unit Tests")
class FileServiceImplTest {

    @Mock private FileRepository fileRepository;
    @Mock private ProjectServiceClient projectServiceClient;

    @InjectMocks private FileServiceImpl fileService;

    private CodeFile buildFile(Integer fileId, Integer projectId, Integer userId) {
        CodeFile f = CodeFile.builder()
                .fileId(fileId)
                .projectId(projectId)
                .name("Main.java")
                .path("/src/Main.java")
                .language("Java")
                .content("public class Main {}")
                .size(20L)
                .createdById(userId)
                .lastEditedBy(userId)
                .isDeleted(false)
                .build();
        return f;
    }

    private ProjectServiceClient.RoleResponse ownerRole() {
        ProjectServiceClient.RoleResponse r = new ProjectServiceClient.RoleResponse();
        r.role = "OWNER";
        return r;
    }

    private ProjectServiceClient.RoleResponse noneRole() {
        ProjectServiceClient.RoleResponse r = new ProjectServiceClient.RoleResponse();
        r.role = "NONE";
        return r;
    }

    // ── getFileById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFileById – returns response when file exists")
    void getFileById_found_returnsResponse() {
        CodeFile file = buildFile(1, 10, 5);
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(file));

        CodeFileResponse response = fileService.getFileById(1);
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Main.java");
    }

    @Test
    @DisplayName("getFileById – throws ResourceNotFoundException when not found")
    void getFileById_notFound_throwsException() {
        when(fileRepository.findByFileIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fileService.getFileById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getFilesByProject ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getFilesByProject – returns list of responses")
    void getFilesByProject_returnsList() {
        CodeFile f1 = buildFile(1, 10, 5);
        CodeFile f2 = buildFile(2, 10, 5);
        f2.setName("Util.java");
        when(fileRepository.findByProjectIdAndIsDeletedFalse(10)).thenReturn(List.of(f1, f2));

        List<CodeFileResponse> result = fileService.getFilesByProject(10);
        assertThat(result).hasSize(2);
    }

    // ── getFileContent ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFileContent – returns file content string")
    void getFileContent_returnsContent() {
        CodeFile file = buildFile(1, 10, 5);
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(file));

        String content = fileService.getFileContent(1);
        assertThat(content).isEqualTo("public class Main {}");
    }

    // ── updateFileContent ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateFileContent – updates content and returns response")
    void updateFileContent_updatesFile() {
        CodeFile file = buildFile(1, 10, 5);
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(file));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CodeFileResponse response = fileService.updateFileContent(1, "new content", 5);
        assertThat(response.getContent()).isEqualTo("new content");
    }

    // ── renameFile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("renameFile – updates name and path")
    void renameFile_updatesNameAndPath() {
        CodeFile file = buildFile(1, 10, 5);
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(file));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CodeFileResponse response = fileService.renameFile(1, "Renamed.java");
        assertThat(response.getName()).isEqualTo("Renamed.java");
    }

    // ── restoreFile ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("restoreFile – sets isDeleted to false")
    void restoreFile_setsNotDeleted() {
        CodeFile file = buildFile(1, 10, 5);
        file.setIsDeleted(true);
        when(fileRepository.findById(1)).thenReturn(Optional.of(file));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CodeFileResponse response = fileService.restoreFile(1);
        assertThat(response).isNotNull();
        verify(fileRepository).save(argThat(f -> !f.getIsDeleted()));
    }

    @Test
    @DisplayName("restoreFile – throws exception when file not found")
    void restoreFile_notFound_throwsException() {
        when(fileRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> fileService.restoreFile(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── moveFile ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("moveFile – updates path to new location")
    void moveFile_updatesPath() {
        CodeFile file = buildFile(1, 10, 5);
        when(fileRepository.findByFileIdAndIsDeletedFalse(1)).thenReturn(Optional.of(file));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CodeFileResponse response = fileService.moveFile(1, "/src/moved/Main.java");
        assertThat(response.getPath()).isEqualTo("src/moved/Main.java");
    }

    // ── getFileTree ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFileTree – returns sorted file list")
    void getFileTree_returnsSortedList() {
        CodeFile f1 = buildFile(1, 10, 5);
        f1.setPath("/src/B.java");
        CodeFile f2 = buildFile(2, 10, 5);
        f2.setPath("/src/A.java");
        when(fileRepository.findByProjectIdAndIsDeletedFalse(10)).thenReturn(List.of(f1, f2));

        List<CodeFileResponse> result = fileService.getFileTree(10);
        assertThat(result).hasSize(2);
        // A.java should come before B.java (sorted)
        assertThat(result.get(0).getPath()).isEqualTo("/src/A.java");
    }

    // ── searchInProject ───────────────────────────────────────────────────────

    @Test
    @DisplayName("searchInProject – returns files matching keyword in name")
    void searchInProject_matchesName() {
        CodeFile file = buildFile(1, 10, 5);
        file.setName("SearchMe.java");
        when(fileRepository.findByProjectIdAndIsDeletedFalse(10)).thenReturn(List.of(file));

        List<CodeFileResponse> result = fileService.searchInProject(10, "searchme");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("searchInProject – returns empty list when no files match")
    void searchInProject_noMatches_returnsEmpty() {
        CodeFile file = buildFile(1, 10, 5);
        file.setName("Unrelated.java");
        file.setContent("nothing here");
        when(fileRepository.findByProjectIdAndIsDeletedFalse(10)).thenReturn(List.of(file));

        List<CodeFileResponse> result = fileService.searchInProject(10, "xyzabcnotfound");
        assertThat(result).isEmpty();
    }

    // ── createFolder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createFolder – creates folder entity when user is owner")
    void createFolder_ownerUser_createsFolder() {
        when(projectServiceClient.getUserRole(10, 5)).thenReturn(ownerRole());
        when(fileRepository.findByProjectIdAndPathAndIsDeletedFalse(eq(10), anyString())).thenReturn(Optional.empty());
        CodeFile savedFolder = CodeFile.builder()
                .fileId(99).projectId(10).name("utils")
                .path("/src/utils/").language("folder")
                .content("").size(0L).isDeleted(false)
                .createdById(5).lastEditedBy(5).build();
        when(fileRepository.save(any())).thenReturn(savedFolder);

        CodeFileResponse result = fileService.createFolder(10, "utils", "/src", 5);
        assertThat(result.getName()).isEqualTo("utils");
        assertThat(result.getLanguage()).isEqualTo("folder");
    }

    @Test
    @DisplayName("createFolder – throws exception when folder already exists")
    void createFolder_alreadyExists_throwsException() {
        when(projectServiceClient.getUserRole(10, 5)).thenReturn(ownerRole());
        CodeFile existing = buildFile(1, 10, 5);
        when(fileRepository.findByProjectIdAndPathAndIsDeletedFalse(eq(10), anyString()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> fileService.createFolder(10, "utils", "/src", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Folder already exists");
    }
}
