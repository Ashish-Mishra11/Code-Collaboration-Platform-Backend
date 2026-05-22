package com.codesync.project.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codesync.project.dto.CollaboratorDto;
import com.codesync.project.dto.CreateProjectDto;
import com.codesync.project.dto.CreateProjectResponseDto;
import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectCollaborator;
import com.codesync.project.feignclient.UserClient;
import com.codesync.project.repository.ProjectCollaboratorRepository;
import com.codesync.project.repository.ProjectRepository;
import com.codesync.project.service.ProjectService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImplementation implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectCollaboratorRepository collaboratorRepository;
    private final UserClient userClient;
    private final ModelMapper modelMapper;

    @Override
    public CreateProjectResponseDto createProject(CreateProjectDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated. Please login first.");
        }
        String username = authentication.getName();
        Long ownerId = userClient.getUserIdByUsername(username);
        if (ownerId == null) {
            throw new RuntimeException("User not found with username: " + username);
        }
        dto.setVisibility(dto.getVisibility().toUpperCase());
        Project project = modelMapper.map(dto, Project.class);
        project.setOwnerId(ownerId.intValue());
        project.setIsArchived(false);
        project.setStarCount(0);
        project.setForkCount(0);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        Project savedProject = projectRepository.save(project);
        return modelMapper.map(savedProject, CreateProjectResponseDto.class);
    }

    @Override
    public CreateProjectResponseDto getProjectById(Integer id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        return modelMapper.map(project, CreateProjectResponseDto.class);
    }

    @Override
    public List<CreateProjectResponseDto> getProjectsByOwner(Integer ownerId) {
        List<Project> projects = projectRepository.findByOwnerId(ownerId);
        return projects.stream()
                .map(p -> modelMapper.map(p, CreateProjectResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<CreateProjectResponseDto> getPublicProjects() {
        return projectRepository.findByVisibility("PUBLIC").stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsArchived()))
                .map(p -> modelMapper.map(p, CreateProjectResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<CreateProjectResponseDto> searchProjects(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        return projectRepository.findByNameContainingIgnoreCase(keyword).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsArchived()))
                .map(p -> modelMapper.map(p, CreateProjectResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public CreateProjectResponseDto updateProject(Integer id, CreateProjectDto dto) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setVisibility(dto.getVisibility());
        existing.setLanguage(dto.getLanguage());
        existing.setUpdatedAt(LocalDateTime.now());
        return modelMapper.map(projectRepository.save(existing), CreateProjectResponseDto.class);
    }

    @Override
    public void archiveProject(Integer id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        project.setIsArchived(true);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    @Override
    public void deleteProject(Integer id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        projectRepository.delete(project);
    }

    @Override
    public CreateProjectResponseDto forkProject(Integer id, Long newOwnerId) {
        Project original = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        if (Boolean.TRUE.equals(original.getIsArchived()))
            throw new RuntimeException("Cannot fork an archived project");

        Project forked = new Project();
        forked.setOwnerId(newOwnerId.intValue());
        forked.setName(original.getName() + " (Fork)");
        forked.setDescription(original.getDescription());
        forked.setLanguage(original.getLanguage());
        forked.setVisibility(original.getVisibility());
        forked.setTemplateId(original.getProjectId());
        forked.setIsArchived(false);
        forked.setStarCount(0);
        forked.setForkCount(0);
        forked.setCreatedAt(LocalDateTime.now());
        forked.setUpdatedAt(LocalDateTime.now());
        Project savedFork = projectRepository.save(forked);
        original.setForkCount(original.getForkCount() + 1);
        projectRepository.save(original);
        return modelMapper.map(savedFork, CreateProjectResponseDto.class);
    }

    @Override
    public void starProject(Integer id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        if (Boolean.TRUE.equals(project.getIsArchived()))
            throw new RuntimeException("Cannot star an archived project");
        project.setStarCount(project.getStarCount() + 1);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    @Override
    public List<CreateProjectResponseDto> getProjectsByLanguage(String language) {
        return projectRepository.findByLanguage(language).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsArchived()))
                .map(p -> modelMapper.map(p, CreateProjectResponseDto.class))
                .collect(Collectors.toList());
    }

    // ── Collaborator management ──────────────────────────────────────────────

    @Override
    public CollaboratorDto addCollaborator(Integer projectId, Integer requesterId, Integer collaboratorId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (!project.getOwnerId().equals(requesterId))
            throw new RuntimeException("Only the project owner can add collaborators.");

        if (collaboratorRepository.countByProjectId(projectId) >= 10)
            throw new RuntimeException("Collaborator limit reached. Max 10 allowed.");

        if (collaboratorId.equals(project.getOwnerId()))
            throw new RuntimeException("Cannot add the project owner as a collaborator.");

        if (collaboratorRepository.findByProjectIdAndUserId(projectId, collaboratorId).isPresent())
            throw new RuntimeException("User is already a collaborator on this project.");

        ProjectCollaborator saved = collaboratorRepository.save(
            ProjectCollaborator.builder()
                .projectId(projectId)
                .userId(collaboratorId)
                .role("COLLABORATOR")
                .addedAt(LocalDateTime.now())
                .build()
        );
        return toDto(saved);
    }

    @Override
    @Transactional
    public void removeCollaborator(Integer projectId, Integer requesterId, Integer collaboratorId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        if (!project.getOwnerId().equals(requesterId))
            throw new RuntimeException("Only the project owner can remove collaborators.");
        collaboratorRepository.deleteByProjectIdAndUserId(projectId, collaboratorId);
    }

    @Override
    public List<CollaboratorDto> getCollaborators(Integer projectId) {
        return collaboratorRepository.findByProjectId(projectId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public String getUserRole(Integer projectId, Integer userId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return "NONE";
        if (project.getOwnerId().equals(userId)) return "OWNER";
        return collaboratorRepository.findByProjectIdAndUserId(projectId, userId)
                .map(c -> "COLLABORATOR")
                .orElse("NONE");
    }

    private CollaboratorDto toDto(ProjectCollaborator c) {
        return CollaboratorDto.builder()
                .id(c.getId())
                .projectId(c.getProjectId())
                .userId(c.getUserId())
                .role(c.getRole())
                .addedAt(c.getAddedAt())
                .build();
    }
}
