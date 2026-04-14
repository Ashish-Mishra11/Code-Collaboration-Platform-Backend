package com.codesync.file.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.codesync.file.entity.CodeFile;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<CodeFile, Integer> {

    List<CodeFile> findByProjectIdAndIsDeletedFalse(Integer projectId);

    Optional<CodeFile> findByFileIdAndIsDeletedFalse(Integer fileId);

    Optional<CodeFile> findByProjectIdAndPathAndIsDeletedFalse(Integer projectId, String path);

    List<CodeFile> findByLanguageAndIsDeletedFalse(String language);

    List<CodeFile> findByLastEditedByAndIsDeletedFalse(Integer lastEditedBy);

    long countByProjectIdAndIsDeletedFalse(Integer projectId);

    List<CodeFile> findByIsDeleted(Boolean isDeleted);

    void deleteByFileId(Integer fileId);
}