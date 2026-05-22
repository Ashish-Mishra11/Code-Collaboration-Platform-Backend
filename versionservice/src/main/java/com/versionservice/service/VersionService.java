package com.versionservice.service;

import com.versionservice.dto.RestoreResponseDto;
import com.versionservice.dto.SnapshotResponseDto;
import com.versionservice.entity.Snapshot;

import java.util.List;
import java.util.Optional;

public interface VersionService {

    Snapshot createSnapshot(Snapshot snapshot);
    Optional<Snapshot> getSnapshotById(int snapshotId);
    List<Snapshot> getSnapshotsByFile(int fileId);
    List<Snapshot> getSnapshotsByProject(int projectId);
    List<Snapshot> getSnapshotsByBranch(int projectId, String branchName);
    Optional<Snapshot> getLatestSnapshot(int fileId);

    /** Returns all distinct branch names that exist for a project */
    List<String> getBranchesByProject(int projectId);

    /** Restore snapshot - returns a DTO (no cross-service CodeFile dependency) */
    RestoreResponseDto restoreSnapshotDto(int snapshotId);

    String diffSnapshots(int snapshotId1, int snapshotId2);
    void createBranch(int projectId, String branchName);
    void tagSnapshot(int snapshotId, String tag);
    List<Snapshot> getFileHistory(int fileId);
    List<SnapshotResponseDto> getFileHistoryDto(int fileId);
}
