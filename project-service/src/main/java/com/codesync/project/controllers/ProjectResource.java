package com.codesync.project.controllers;



import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codesync.project.dto.CreateProjectDto;
import com.codesync.project.dto.CreateProjectResponseDto;
import com.codesync.project.entity.Project;
import com.codesync.project.service.ProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectResource {

    private final ProjectService projectService;

    //  CREATE PROJECT
    @PostMapping
    public ResponseEntity<CreateProjectResponseDto> create(@RequestBody CreateProjectDto createProjectDto) {
        CreateProjectResponseDto createdProjectResponseDto = projectService.createProject(createProjectDto);
        return new ResponseEntity<>(createdProjectResponseDto, HttpStatus.CREATED);
    }

    // GET PROJECT BY ID
    @GetMapping("/{id}")
    public ResponseEntity<CreateProjectResponseDto> getById(@PathVariable Integer id) {
        CreateProjectResponseDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    //  GET PROJECTS BY OWNER
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<CreateProjectResponseDto>> getByOwner(@PathVariable Integer ownerId) {
        List<CreateProjectResponseDto> projects = projectService.getProjectsByOwner(ownerId);
        return ResponseEntity.ok(projects);
    }

    //  GET PUBLIC PROJECTS
    @GetMapping("/public")
    public ResponseEntity<List<CreateProjectResponseDto>> getPublic() {
        List<CreateProjectResponseDto> projects = projectService.getPublicProjects();
        return ResponseEntity.ok(projects);
    }

    //  SEARCH PROJECTS
    @GetMapping("/search")
    public ResponseEntity<List<CreateProjectResponseDto>> search(@RequestParam String keyword) {
        List<CreateProjectResponseDto> projects = projectService.searchProjects(keyword);
        return ResponseEntity.ok(projects);
    }

    //  GET PROJECTS BY MEMBER (if implemented in service)
//    @GetMapping("/member/{userId}")
//    public ResponseEntity<List<Project>> getByMember(@PathVariable String userId) {
//        List<Project> projects = projectService.getProjectsByMember(userId);
//        return ResponseEntity.ok(projects);
//    }

    
 // GET PROJECTS BY LANGUAGE
    @GetMapping("/language/{language}")
    public ResponseEntity<List<CreateProjectResponseDto>> getByLanguage(@PathVariable String language) {
        List<CreateProjectResponseDto> projects = projectService.getProjectsByLanguage(language);
        return ResponseEntity.ok(projects);
    }
    
    //  UPDATE PROJECT
    @PutMapping("/{id}")
    public ResponseEntity<CreateProjectResponseDto> update(@PathVariable Integer id,
                                          @RequestBody CreateProjectDto createProjectDto) {
    	CreateProjectResponseDto updatedProject = projectService.updateProject(id, createProjectDto);
        return ResponseEntity.ok(updatedProject);
    }

    //  ARCHIVE PROJECT
    @PutMapping("/{id}/archive")
    public ResponseEntity<String> archive(@PathVariable Integer id) {
        projectService.archiveProject(id);
        return ResponseEntity.ok("Project archived successfully");
    }

    //  DELETE PROJECT
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }
    
 // FORK PROJECT
    @PostMapping("/{id}/fork")
    public ResponseEntity<CreateProjectResponseDto> fork(@PathVariable Integer id,
                                                         @RequestParam Long newOwnerId) {
        CreateProjectResponseDto forkedProject = projectService.forkProject(id, newOwnerId);
        return new ResponseEntity<>(forkedProject, HttpStatus.CREATED);
    }

    // STAR PROJECT
    @PostMapping("/{id}/star")
    public ResponseEntity<String> star(@PathVariable Integer id) {
        projectService.starProject(id);
        return ResponseEntity.ok("Project starred successfully");
    }
    
}