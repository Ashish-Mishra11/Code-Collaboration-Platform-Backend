package com.codesync.project;

import com.codesync.project.dto.CreateProjectDto;
import com.codesync.project.dto.CreateProjectResponseDto;
import com.codesync.project.entity.Project;
import com.codesync.project.feignclient.UserClient;
import com.codesync.project.repository.ProjectRepository;
import com.codesync.project.service.implementation.ProjectServiceImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceApplicationTests {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserClient userClient;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private ProjectServiceImplementation projectService;

    private Project sampleProject;
    private CreateProjectDto createDto;
    private CreateProjectResponseDto responseDto;

    @BeforeEach
    void setUp() {
        sampleProject = new Project();
        sampleProject.setProjectId(1);
        sampleProject.setOwnerId(10);
        sampleProject.setName("CodeSync App");
        sampleProject.setDescription("A collaborative IDE");
        sampleProject.setLanguage("Java");
        sampleProject.setVisibility("PUBLIC");
        sampleProject.setIsArchived(false);
        sampleProject.setStarCount(0);
        sampleProject.setForkCount(0);
        sampleProject.setCreatedAt(LocalDateTime.now());
        sampleProject.setUpdatedAt(LocalDateTime.now());

        createDto = new CreateProjectDto();
        createDto.setName("CodeSync App");
        createDto.setDescription("A collaborative IDE");
        createDto.setLanguage("Java");
        createDto.setVisibility("public");

        responseDto = new CreateProjectResponseDto();
        responseDto.setProjectId(1L);
        responseDto.setName("CodeSync App");

        // Set up Spring Security context for tests that need authentication
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("testuser", null, List.of())
        );
    }

    // ── createProject ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProject: creates project with owner from auth context")
    void createProject_success() {
        when(userClient.getUserIdByUsername("testuser")).thenReturn(10L);
        when(modelMapper.map(any(CreateProjectDto.class), eq(Project.class))).thenReturn(sampleProject);
        when(projectRepository.save(any())).thenReturn(sampleProject);
        when(modelMapper.map(any(Project.class), eq(CreateProjectResponseDto.class))).thenReturn(responseDto);

        CreateProjectResponseDto result = projectService.createProject(createDto);

        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(1);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("createProject: visibility is uppercased before saving")
    void createProject_visibilityUppercased() {
        when(userClient.getUserIdByUsername("testuser")).thenReturn(10L);
        when(modelMapper.map(any(CreateProjectDto.class), eq(Project.class))).thenReturn(sampleProject);
        when(projectRepository.save(any())).thenReturn(sampleProject);
        when(modelMapper.map(any(Project.class), eq(CreateProjectResponseDto.class))).thenReturn(responseDto);

        createDto.setVisibility("private");
        projectService.createProject(createDto);

        assertThat(createDto.getVisibility()).isEqualTo("PRIVATE");
    }

    @Test
    @DisplayName("createProject: throws when user not found by feign client")
    void createProject_userNotFound() {
        when(userClient.getUserIdByUsername("testuser")).thenReturn(null);

        assertThatThrownBy(() -> projectService.createProject(createDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(projectRepository, never()).save(any());
    }

    // ── getProjectById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectById: returns mapped DTO when project exists")
    void getProjectById_found() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(sampleProject));
        when(modelMapper.map(any(Project.class), eq(CreateProjectResponseDto.class))).thenReturn(responseDto);

        CreateProjectResponseDto result = projectService.getProjectById(1);

        assertThat(result.getProjectId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getProjectById: throws RuntimeException when project not found")
    void getProjectById_notFound() {
        when(projectRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found");
    }

    // ── getProjectsByOwner ────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectsByOwner: returns all projects for owner")
    void getProjectsByOwner() {
        when(projectRepository.findByOwnerId(10)).thenReturn(List.of(sampleProject));
        when(modelMapper.map(any(Project.class), eq(CreateProjectResponseDto.class))).thenReturn(responseDto);

        List<CreateProjectResponseDto> result = projectService.getProjectsByOwner(10);

        assertThat(result).hasSize(1);
    }

    // ── getPublicProjects ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getPublicProjects: excludes archived projects")
    void getPublicProjects_excludesArchived() {
        Project archived = new Project();
        archived.setProjectId(2);
        archived.setVisibility("PUBLIC");
        archived.setIsArchived(true);

        when(projectRepository.findByVisibility("PUBLIC")).thenReturn(List.of(sampleProject, archived));
        when(modelMapper.map(eq(sampleProject), eq(CreateProjectResponseDto.class))).thenReturn(responseDto);

        List<CreateProjectResponseDto> result = projectService.getPublicProjects();

        assertThat(result).hasSize(1);
        verify(modelMapper, times(1)).map(any(Project.class), eq(CreateProjectResponseDto.class));
    }

    // ── searchProjects ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchProjects: returns empty list for blank keyword")
    void searchProjects_blankKeyword() {
        assertThat(projectService.searchProjects("")).isEmpty();
        assertThat(projectService.searchProjects("  ")).isEmpty();
        verifyNoInteractions(projectRepository);
    }

    @Test
    @DisplayName("searchProjects: returns matching non-archived projects")
    void searchProjects_withKeyword() {
        when(projectRepository.findByNameContainingIgnoreCase("code")).thenReturn(List.of(sampleProject));
        when(modelMapper.map(any(Project.class), eq(CreateProjectResponseDto.class))).thenReturn(responseDto);

        List<CreateProjectResponseDto> result = projectService.searchProjects("code");

        assertThat(result).hasSize(1);
    }

    // ── archiveProject ────────────────────────────────────────────────────────

    @Test
    @DisplayName("archiveProject: sets isArchived=true")
    void archiveProject_success() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any())).thenReturn(sampleProject);

        projectService.archiveProject(1);

        assertThat(sampleProject.getIsArchived()).isTrue();
        verify(projectRepository).save(sampleProject);
    }

    // ── forkProject ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("forkProject: creates copy with new owner and increments original forkCount")
    void forkProject_success() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            if (p.getProjectId() == null) p.setProjectId(99);
            return p;
        });
        when(modelMapper.map(any(Project.class), eq(CreateProjectResponseDto.class))).thenReturn(responseDto);

        projectService.forkProject(1, 20L);

        // Fork saved + original forkCount updated
        verify(projectRepository, times(2)).save(any(Project.class));
        assertThat(sampleProject.getForkCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("forkProject: throws when trying to fork an archived project")
    void forkProject_archivedThrows() {
        sampleProject.setIsArchived(true);
        when(projectRepository.findById(1)).thenReturn(Optional.of(sampleProject));

        assertThatThrownBy(() -> projectService.forkProject(1, 20L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("archived");
    }

    // ── starProject ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("starProject: increments starCount by 1")
    void starProject_success() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any())).thenReturn(sampleProject);

        projectService.starProject(1);

        assertThat(sampleProject.getStarCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("starProject: throws when project is archived")
    void starProject_archived() {
        sampleProject.setIsArchived(true);
        when(projectRepository.findById(1)).thenReturn(Optional.of(sampleProject));

        assertThatThrownBy(() -> projectService.starProject(1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("archived");
    }

    // ── deleteProject ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProject: hard-deletes the project entity")
    void deleteProject_success() {
        when(projectRepository.findById(1)).thenReturn(Optional.of(sampleProject));
        doNothing().when(projectRepository).delete(sampleProject);

        projectService.deleteProject(1);

        verify(projectRepository).delete(sampleProject);
    }

    @Test
    @DisplayName("deleteProject: throws when project not found")
    void deleteProject_notFound() {
        when(projectRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found");
    }
}
