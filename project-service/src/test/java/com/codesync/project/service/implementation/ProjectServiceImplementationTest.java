package com.codesync.project.service.implementation;

import com.codesync.project.dto.CollaboratorDto;
import com.codesync.project.dto.CreateProjectDto;
import com.codesync.project.dto.CreateProjectResponseDto;
import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectCollaborator;
import com.codesync.project.feignclient.UserClient;
import com.codesync.project.repository.ProjectCollaboratorRepository;
import com.codesync.project.repository.ProjectRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
class ProjectServiceImplementationTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectCollaboratorRepository collaboratorRepository;
    @Mock private UserClient userClient;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private ProjectServiceImplementation projectService;

    private Project buildProject(Integer id, Integer ownerId) {
        Project p = new Project();
        p.setProjectId(id);
        p.setOwnerId(ownerId);
        p.setName("TestProject");
        p.setDescription("A test project");
        p.setLanguage("Java");
        p.setVisibility("PUBLIC");
        p.setIsArchived(false);
        p.setStarCount(0);
        p.setForkCount(0);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    // ── getProjectById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectById – returns DTO when project found")
    void getProjectById_found_returnsDto() {
        Project project = buildProject(1, 10);
        CreateProjectResponseDto dto = new CreateProjectResponseDto();
        dto.setName("TestProject");

        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(modelMapper.map(project, CreateProjectResponseDto.class)).thenReturn(dto);

        CreateProjectResponseDto result = projectService.getProjectById(1);
        assertThat(result.getName()).isEqualTo("TestProject");
    }

    @Test
    @DisplayName("getProjectById – throws RuntimeException when project not found")
    void getProjectById_notFound_throwsException() {
        when(projectRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.getProjectById(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found with id: 999");
    }

    // ── getProjectsByOwner ────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectsByOwner – returns mapped DTOs")
    void getProjectsByOwner_returnsMappedDtos() {
        Project p1 = buildProject(1, 10);
        Project p2 = buildProject(2, 10);
        CreateProjectResponseDto d1 = new CreateProjectResponseDto();
        CreateProjectResponseDto d2 = new CreateProjectResponseDto();

        when(projectRepository.findByOwnerId(10)).thenReturn(List.of(p1, p2));
        when(modelMapper.map(p1, CreateProjectResponseDto.class)).thenReturn(d1);
        when(modelMapper.map(p2, CreateProjectResponseDto.class)).thenReturn(d2);

        List<CreateProjectResponseDto> result = projectService.getProjectsByOwner(10);
        assertThat(result).hasSize(2);
    }

    // ── getPublicProjects ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getPublicProjects – returns only non-archived public projects")
    void getPublicProjects_filtersArchived() {
        Project active = buildProject(1, 10);
        Project archived = buildProject(2, 10);
        archived.setIsArchived(true);

        CreateProjectResponseDto activeDto = new CreateProjectResponseDto();
        when(projectRepository.findByVisibility("PUBLIC")).thenReturn(List.of(active, archived));
        when(modelMapper.map(active, CreateProjectResponseDto.class)).thenReturn(activeDto);

        List<CreateProjectResponseDto> result = projectService.getPublicProjects();
        assertThat(result).hasSize(1);
    }

    // ── searchProjects ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchProjects – returns empty list for null keyword")
    void searchProjects_nullKeyword_returnsEmpty() {
        List<CreateProjectResponseDto> result = projectService.searchProjects(null);
        assertThat(result).isEmpty();
        verify(projectRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("searchProjects – returns matching non-archived projects")
    void searchProjects_validKeyword_returnsMatches() {
        Project project = buildProject(1, 10);
        CreateProjectResponseDto dto = new CreateProjectResponseDto();

        when(projectRepository.findByNameContainingIgnoreCase("Test")).thenReturn(List.of(project));
        when(modelMapper.map(project, CreateProjectResponseDto.class)).thenReturn(dto);

        List<CreateProjectResponseDto> result = projectService.searchProjects("Test");
        assertThat(result).hasSize(1);
    }

    // ── updateProject ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProject – updates fields and returns DTO")
    void updateProject_found_updatesAndReturnsDto() {
        Project project = buildProject(1, 10);
        CreateProjectDto updateDto = new CreateProjectDto();
        updateDto.setName("Updated");
        updateDto.setDescription("Updated desc");
        updateDto.setVisibility("PRIVATE");
        updateDto.setLanguage("Python");

        CreateProjectResponseDto returnDto = new CreateProjectResponseDto();
        returnDto.setName("Updated");

        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);
        when(modelMapper.map(project, CreateProjectResponseDto.class)).thenReturn(returnDto);

        CreateProjectResponseDto result = projectService.updateProject(1, updateDto);
        assertThat(result.getName()).isEqualTo("Updated");
    }

    // ── archiveProject ────────────────────────────────────────────────────────

    @Test
    @DisplayName("archiveProject – sets isArchived to true")
    void archiveProject_found_archives() {
        Project project = buildProject(1, 10);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);

        projectService.archiveProject(1);
        verify(projectRepository).save(argThat(p -> Boolean.TRUE.equals(p.getIsArchived())));
    }

    @Test
    @DisplayName("archiveProject – throws exception when project not found")
    void archiveProject_notFound_throwsException() {
        when(projectRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> projectService.archiveProject(999))
                .isInstanceOf(RuntimeException.class);
    }

    // ── deleteProject ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProject – calls repository delete")
    void deleteProject_found_deletesProject() {
        Project project = buildProject(1, 10);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));

        projectService.deleteProject(1);
        verify(projectRepository).delete(project);
    }

    // ── forkProject ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("forkProject – creates fork with incremented forkCount")
    void forkProject_success_createsForkedProject() {
        Project original = buildProject(1, 10);
        original.setForkCount(2);
        Project forked = buildProject(2, 20);
        CreateProjectResponseDto forkDto = new CreateProjectResponseDto();

        when(projectRepository.findById(1)).thenReturn(Optional.of(original));
        when(projectRepository.save(any())).thenReturn(forked);
        when(modelMapper.map(forked, CreateProjectResponseDto.class)).thenReturn(forkDto);

        CreateProjectResponseDto result = projectService.forkProject(1, 20L);
        assertThat(result).isNotNull();
        verify(projectRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("forkProject – throws exception for archived project")
    void forkProject_archived_throwsException() {
        Project archived = buildProject(1, 10);
        archived.setIsArchived(true);
        when(projectRepository.findById(1)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> projectService.forkProject(1, 20L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot fork an archived project");
    }

    // ── starProject ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("starProject – increments star count")
    void starProject_found_incrementsStarCount() {
        Project project = buildProject(1, 10);
        project.setStarCount(5);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);

        projectService.starProject(1);
        verify(projectRepository).save(argThat(p -> p.getStarCount() == 6));
    }

    // ── getProjectsByLanguage ─────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectsByLanguage – returns non-archived projects for language")
    void getProjectsByLanguage_returnsFilteredProjects() {
        Project p = buildProject(1, 10);
        CreateProjectResponseDto dto = new CreateProjectResponseDto();
        when(projectRepository.findByLanguage("Java")).thenReturn(List.of(p));
        when(modelMapper.map(p, CreateProjectResponseDto.class)).thenReturn(dto);

        List<CreateProjectResponseDto> result = projectService.getProjectsByLanguage("Java");
        assertThat(result).hasSize(1);
    }

    // ── addCollaborator ───────────────────────────────────────────────────────

    @Test
    @DisplayName("addCollaborator – adds collaborator when requester is owner")
    void addCollaborator_ownerRequest_success() {
        Project project = buildProject(1, 10);
        ProjectCollaborator collab = ProjectCollaborator.builder()
                .id(1).projectId(1).userId(99).role("COLLABORATOR")
                .addedAt(LocalDateTime.now()).build();

        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(collaboratorRepository.countByProjectId(1)).thenReturn(0);
        when(collaboratorRepository.findByProjectIdAndUserId(1, 99)).thenReturn(Optional.empty());
        when(collaboratorRepository.save(any())).thenReturn(collab);

        CollaboratorDto result = projectService.addCollaborator(1, 10, 99);
        assertThat(result.getUserId()).isEqualTo(99);
    }

    @Test
    @DisplayName("addCollaborator – throws exception when requester is not owner")
    void addCollaborator_notOwner_throwsException() {
        Project project = buildProject(1, 10);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.addCollaborator(1, 99, 55))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only the project owner can add collaborators");
    }

    // ── removeCollaborator ────────────────────────────────────────────────────

    @Test
    @DisplayName("removeCollaborator – calls delete when requester is owner")
    void removeCollaborator_ownerRequest_callsDelete() {
        Project project = buildProject(1, 10);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));

        projectService.removeCollaborator(1, 10, 99);
        verify(collaboratorRepository).deleteByProjectIdAndUserId(1, 99);
    }

    // ── getCollaborators ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getCollaborators – returns list of collaborator DTOs")
    void getCollaborators_returnsList() {
        ProjectCollaborator c = ProjectCollaborator.builder()
                .id(1).projectId(1).userId(99).role("COLLABORATOR")
                .addedAt(LocalDateTime.now()).build();
        when(collaboratorRepository.findByProjectId(1)).thenReturn(List.of(c));

        List<CollaboratorDto> result = projectService.getCollaborators(1);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(99);
    }

    // ── getUserRole ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserRole – returns OWNER when userId matches ownerId")
    void getUserRole_isOwner_returnsOwner() {
        Project project = buildProject(1, 10);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));

        assertThat(projectService.getUserRole(1, 10)).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("getUserRole – returns COLLABORATOR when user is collaborator")
    void getUserRole_isCollaborator_returnsCollaborator() {
        Project project = buildProject(1, 10);
        ProjectCollaborator collab = ProjectCollaborator.builder().userId(99).role("COLLABORATOR").build();
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(collaboratorRepository.findByProjectIdAndUserId(1, 99)).thenReturn(Optional.of(collab));

        assertThat(projectService.getUserRole(1, 99)).isEqualTo("COLLABORATOR");
    }

    @Test
    @DisplayName("getUserRole – returns NONE when project not found")
    void getUserRole_projectNotFound_returnsNone() {
        when(projectRepository.findById(999)).thenReturn(Optional.empty());
        assertThat(projectService.getUserRole(999, 10)).isEqualTo("NONE");
    }

    @Test
    @DisplayName("getUserRole – returns NONE when user is neither owner nor collaborator")
    void getUserRole_stranger_returnsNone() {
        Project project = buildProject(1, 10);
        when(projectRepository.findById(1)).thenReturn(Optional.of(project));
        when(collaboratorRepository.findByProjectIdAndUserId(1, 55)).thenReturn(Optional.empty());

        assertThat(projectService.getUserRole(1, 55)).isEqualTo("NONE");
    }
}
