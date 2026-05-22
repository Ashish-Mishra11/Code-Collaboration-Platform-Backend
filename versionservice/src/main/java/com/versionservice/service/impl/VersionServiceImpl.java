package com.versionservice.service.impl;

import com.versionservice.dto.RestoreResponseDto;
import com.versionservice.dto.SnapshotResponseDto;
import com.versionservice.entity.Snapshot;
import com.versionservice.repository.SnapshotRepository;
import com.versionservice.service.VersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VersionServiceImpl implements VersionService {

    private final SnapshotRepository snapshotRepository;

    @Override
    public Snapshot createSnapshot(Snapshot snapshot) {
        if (snapshot.getSnapshotId() != null && snapshotRepository.existsById(snapshot.getSnapshotId())) {
            throw new IllegalArgumentException("Snapshot with id " + snapshot.getSnapshotId() + " already exists.");
        }
        snapshot.setCreatedAt(LocalDateTime.now());
        // Default branch to 'main' if not provided
        if (snapshot.getBranch() == null || snapshot.getBranch().isBlank()) {
            snapshot.setBranch("main");
        }
        Snapshot saved = snapshotRepository.save(snapshot);
        log.info("Created snapshot ID: {} for file ID: {} in project: {} branch: {}",
                saved.getSnapshotId(), saved.getFileId(), saved.getProjectId(), saved.getBranch());
        return saved;
    }

    @Override
    public Optional<Snapshot> getSnapshotById(int snapshotId) {
        return snapshotRepository.findById(snapshotId);
    }

    @Override
    public List<Snapshot> getSnapshotsByFile(int fileId) {
        return snapshotRepository.findByFileIdOrderByCreatedAtDesc(fileId);
    }

    @Override
    public List<Snapshot> getSnapshotsByProject(int projectId) {
        return snapshotRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Override
    public List<Snapshot> getSnapshotsByBranch(int projectId, String branchName) {
        return snapshotRepository.findByProjectIdAndBranchOrderByCreatedAtDesc(projectId, branchName);
    }

    @Override
    public Optional<Snapshot> getLatestSnapshot(int fileId) {
        return snapshotRepository.findTopByFileIdOrderByCreatedAtDesc(fileId);
    }

    /**
     * FIXED: Returns all distinct branch names for a project.
     * Derives branches from existing snapshots (no separate Branch table needed).
     */
    @Override
    public List<String> getBranchesByProject(int projectId) {
        List<String> branches = snapshotRepository.findDistinctBranchesByProjectId(projectId);
        // Always include 'main' even if empty
        if (!branches.contains("main")) {
            branches = new java.util.ArrayList<>(branches);
            branches.add(0, "main");
        }
        log.info("Fetched {} branches for project {}", branches.size(), projectId);
        return branches;
    }

    /**
     * FIXED: Restore snapshot - returns DTO directly, no cross-service CodeFile dependency.
     */
    @Override
    public RestoreResponseDto restoreSnapshotDto(int snapshotId) {
        Snapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot not found with id: " + snapshotId));

        Snapshot restored = Snapshot.builder()
                .projectId(snapshot.getProjectId())
                .fileId(snapshot.getFileId())
                .authorId(snapshot.getAuthorId())
                .message("Restored from snapshot " + snapshotId)
                .content(snapshot.getContent())
                .hash(snapshot.getHash())
                .parentSnapshotId(snapshotId)
                .branch(snapshot.getBranch())
                .createdAt(LocalDateTime.now())
                .build();

        Snapshot saved = snapshotRepository.save(restored);
        log.info("Restored snapshot {} as new snapshot {}", snapshotId, saved.getSnapshotId());

        return RestoreResponseDto.builder()
                .restoredFromSnapshotId(snapshotId)
                .content(snapshot.getContent())
                .message("Snapshot " + snapshotId + " restored as a new snapshot (non-destructive)")
                .build();
    }

    @Override
    public String diffSnapshots(int snapshotId1, int snapshotId2) {
        Snapshot s1 = snapshotRepository.findById(snapshotId1)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId1));
        Snapshot s2 = snapshotRepository.findById(snapshotId2)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId2));
        return String.format(
                "Diff between snapshot %d (branch: %s) and %d (branch: %s)\nContent length difference: %d characters",
                snapshotId1, s1.getBranch(), snapshotId2, s2.getBranch(),
                s1.getContent().length() - s2.getContent().length());
    }

    @Override
    public void createBranch(int projectId, String branchName) {
        // Branch existence is implicit - just log. Actual branching happens when
        // a snapshot is saved with that branchName.
        log.info("Branch '{}' registered for project {}", branchName, projectId);
    }

    @Override
    public void tagSnapshot(int snapshotId, String tag) {
        Snapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        snapshot.setTag(tag);
        snapshotRepository.save(snapshot);
        log.info("Tagged snapshot {} with '{}'", snapshotId, tag);
    }

    @Override
    public List<Snapshot> getFileHistory(int fileId) {
        return snapshotRepository.findByFileIdOrderByCreatedAtDesc(fileId);
    }

    @Override
    public List<SnapshotResponseDto> getFileHistoryDto(int fileId) {
        return getFileHistory(fileId).stream().map(this::convertToDto).toList();
    }

    private SnapshotResponseDto convertToDto(Snapshot s) {
        return SnapshotResponseDto.builder()
                .snapshotId(s.getSnapshotId())
                .projectId(s.getProjectId())
                .fileId(s.getFileId())
                .authorId(s.getAuthorId())
                .message(s.getMessage())
                .hash(s.getHash())
                .branch(s.getBranch())
                .tag(s.getTag())
                .createdAt(s.getCreatedAt())
                .content(s.getContent())
                .build();
    }
}
