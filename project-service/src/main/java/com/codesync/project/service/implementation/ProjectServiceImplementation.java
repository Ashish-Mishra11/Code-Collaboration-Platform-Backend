package com.codesync.project.service.implementation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.codesync.project.dto.CreateProjectDto;
import com.codesync.project.dto.CreateProjectResponseDto;
import com.codesync.project.entity.Project;
import com.codesync.project.feignclient.UserClient;
import com.codesync.project.repository.ProjectRepository;
import com.codesync.project.service.ProjectService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class ProjectServiceImplementation implements ProjectService {
	
	
	private final ProjectRepository projectRepository;
    private final UserClient userClient;           // Feign Client
    private final ModelMapper modelMapper;         // or use MapStruct

	@Override
	public CreateProjectResponseDto createProject(CreateProjectDto dto) {
		// TODO Auto-generated method stub
		// Step 1: Get authenticated username from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated. Please login first.");
        }

        String username = authentication.getName();
//        @Nullable
//		Object credentials = authentication.getCredentials();
//
//        String jwtString=(String)credentials;
        // Step 2: Fetch userId from Auth Service using Feign Client
        Long ownerId = userClient.getUserIdByUsername(username);

        if (ownerId == null) {
            throw new RuntimeException("User not found with username: " + username);
        }

        dto.setVisibility(dto.getVisibility().toUpperCase());
        // Step 3: Convert DTO to Entity
        Project project = modelMapper.map(dto, Project.class);

        // Step 4: Set owner and default values
        project.setOwnerId(ownerId.intValue());   // Entity uses Integer
        project.setIsArchived(false);
        project.setStarCount(0);
        project.setForkCount(0);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        // Step 5: Save project
        Project savedProject = projectRepository.save(project);

        // Step 6: Convert to Response DTO
        return modelMapper.map(savedProject, CreateProjectResponseDto.class);
		
		
	}

	@Override
	public CreateProjectResponseDto getProjectById(Integer id) {
		// TODO Auto-generated method stub
		Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        
        return modelMapper.map(project, CreateProjectResponseDto.class);
		
	}

	@Override
	public List<CreateProjectResponseDto> getProjectsByOwner(Integer ownerId) {
		// TODO Auto-generated method stub
		List<Project> projects = projectRepository.findByOwnerId(ownerId);

        return projects.stream()
                .map(project -> modelMapper.map(project, CreateProjectResponseDto.class))
                .collect(Collectors.toList());
		
	}

	@Override
	public List<CreateProjectResponseDto> getPublicProjects() {
		// TODO Auto-generated method stub
		// Only non-archived public projects
        List<Project> projects = projectRepository.findByVisibility("PUBLIC")
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsArchived()))
                .collect(Collectors.toList());

        return projects.stream()
                .map(project -> modelMapper.map(project, CreateProjectResponseDto.class))
                .collect(Collectors.toList());
		
	}

	@Override
	public List<CreateProjectResponseDto> searchProjects(String keyword) {
		// TODO Auto-generated method stub
		if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        // Search in name and description (case-insensitive)
        // Note: Repository has findByNameContainingIgnoreCase, but for description we need custom query or combined logic
        List<Project> projects = projectRepository.findByNameContainingIgnoreCase(keyword);

        // You can improve this by adding a custom @Query in repository for name OR description
        List<Project> filtered = projects.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsArchived()))
                .collect(Collectors.toList());

        return filtered.stream()
                .map(project -> modelMapper.map(project, CreateProjectResponseDto.class))
                .collect(Collectors.toList());
		
	}

	@Override
	public CreateProjectResponseDto updateProject(Integer id, CreateProjectDto dto) {
		// TODO Auto-generated method stub
		Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        // Optional: Check if current user is owner (recommended for security)
        // Integer currentOwnerId = getCurrentOwnerId();
        // if (!existingProject.getOwnerId().equals(currentOwnerId)) {
        //     throw new RuntimeException("You are not authorized to update this project");
        // }

        // Update only fields from DTO
        existingProject.setName(dto.getName());
        existingProject.setDescription(dto.getDescription());
        existingProject.setVisibility(dto.getVisibility());
        existingProject.setLanguage(dto.getLanguage());
        existingProject.setUpdatedAt(LocalDateTime.now());

        Project updatedProject = projectRepository.save(existingProject);

        return modelMapper.map(updatedProject, CreateProjectResponseDto.class);
		
	}

	@Override
	public void archiveProject(Integer id) {
		// TODO Auto-generated method stub
		Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        // Optional: Check ownership here

        project.setIsArchived(true);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
		
	}

	@Override
	public void deleteProject(Integer id) {
		// TODO Auto-generated method stub
		Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        // Optional: Check if owner or admin

        projectRepository.delete(project);  // Hard delete
		
	}

	@Override
	public CreateProjectResponseDto forkProject(Integer id, Long newOwnerId) {
		// TODO Auto-generated method stub
		Project original = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        if (Boolean.TRUE.equals(original.getIsArchived())) {
            throw new RuntimeException("Cannot fork an archived project");
        }

        Project forked = new Project();
        forked.setOwnerId(newOwnerId.intValue());
        forked.setName(original.getName() + " (Fork)");
        forked.setDescription(original.getDescription());
        forked.setLanguage(original.getLanguage());
        forked.setVisibility(original.getVisibility());
        forked.setTemplateId(original.getProjectId()); // Link to original as template
        forked.setIsArchived(false);
        forked.setStarCount(0);
        forked.setForkCount(0);
        forked.setCreatedAt(LocalDateTime.now());
        forked.setUpdatedAt(LocalDateTime.now());

        Project savedFork = projectRepository.save(forked);

        // Optionally increment original forkCount
        original.setForkCount(original.getForkCount() + 1);
        projectRepository.save(original);

        return modelMapper.map(savedFork, CreateProjectResponseDto.class);
		
	}

	@Override
	public void starProject(Integer id) {
		// TODO Auto-generated method stub
		Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        if (Boolean.TRUE.equals(project.getIsArchived())) {
            throw new RuntimeException("Cannot star an archived project");
        }

        project.setStarCount(project.getStarCount() + 1);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
		
	}

	@Override
	public List<CreateProjectResponseDto> getProjectsByLanguage(String language) {
		// TODO Auto-generated method stub
		List<Project> projects = projectRepository.findByLanguage(language)
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsArchived()))
                .collect(Collectors.toList());

        return projects.stream()
                .map(project -> modelMapper.map(project, CreateProjectResponseDto.class))
                .collect(Collectors.toList());
		
	}



}
