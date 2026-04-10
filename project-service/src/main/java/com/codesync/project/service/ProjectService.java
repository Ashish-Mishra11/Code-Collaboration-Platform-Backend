package com.codesync.project.service;

import java.util.List;

import com.codesync.project.dto.CreateProjectDto;
import com.codesync.project.dto.CreateProjectResponseDto;


public interface ProjectService {

    CreateProjectResponseDto createProject(CreateProjectDto dto);

    CreateProjectResponseDto getProjectById(Integer id);

    List<CreateProjectResponseDto> getProjectsByOwner(Integer ownerId);

    List<CreateProjectResponseDto> getPublicProjects();

    List<CreateProjectResponseDto> searchProjects(String keyword);

    CreateProjectResponseDto updateProject(Integer id, CreateProjectDto dto);

    void archiveProject(Integer id);

    void deleteProject(Integer id);

    CreateProjectResponseDto forkProject(Integer id, Long newOwnerId);

    void starProject(Integer id);

    List<CreateProjectResponseDto> getProjectsByLanguage(String language);
}