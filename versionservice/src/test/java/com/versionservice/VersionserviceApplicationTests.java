package com.versionservice;

import com.versionservice.dto.RestoreResponseDto;
import com.versionservice.dto.SnapshotResponseDto;
import com.versionservice.entity.Snapshot;
import com.versionservice.repository.SnapshotRepository;
import com.versionservice.service.impl.VersionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VersionService Unit Tests")
class VersionserviceApplicationTests {

    @Mock
    private SnapshotRepository snapshotRepository;

    @InjectMocks
    private VersionServiceImpl versionService;

    private Snapshot sampleSnapshot;

    @BeforeEach
    void setUp() {
        sampleSnapshot = Snapshot.builder()
                .snapshotId(1)
                .projectId(10)
                .fileId(100)
                .authorId(5)
                .message("Initial commit")
                .content("public class Main {}")
                .branch("main")
                .hash("abc123")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── createSnapshot ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSnapshot: saves and returns snapshot with createdAt set")
    void createSnapshot_success() {
        Snapshot input = Snapshot.builder()
                .projectId(10).fileId(100).authorId(5)
                .message("Test").content("code").branch("feature/branch").build();

        when(snapshotRepository.save(any())).thenAnswer(inv -> {
            Snapshot s = inv.getArgument(0);
            s.setSnapshotId(42);
            return s;
        });

        Snapshot saved = versionService.createSnapshot(input);

        assertThat(saved.getSnapshotId()).isEqualTo(42);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getBranch()).isEqualTo("feature/branch");
    }

    @Test
    @DisplayName("createSnapshot: defaults to 'main' branch when not provided")
    void createSnapshot_defaultsBranchToMain() {
        Snapshot input = Snapshot.builder()
                .projectId(10).fileId(100).authorId(5).message("No branch").content("code").build();

        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Snapshot saved = versionService.createSnapshot(input);
        assertThat(saved.getBranch()).isEqualTo("main");
    }

    @Test
    @DisplayName("createSnapshot: throws when snapshot with same ID already exists")
    void createSnapshot_duplicateId() {
        sampleSnapshot.setSnapshotId(1);
        when(snapshotRepository.existsById(1)).thenReturn(true);

        assertThatThrownBy(() -> versionService.createSnapshot(sampleSnapshot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // ── getSnapshotById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSnapshotById: returns snapshot when found")
    void getSnapshotById_found() {
        when(snapshotRepository.findById(1)).thenReturn(Optional.of(sampleSnapshot));
        assertThat(versionService.getSnapshotById(1)).isPresent();
    }

    @Test
    @DisplayName("getSnapshotById: returns empty when not found")
    void getSnapshotById_notFound() {
        when(snapshotRepository.findById(99)).thenReturn(Optional.empty());
        assertThat(versionService.getSnapshotById(99)).isEmpty();
    }

    // ── getSnapshotsByBranch ───────────────────────────────────────────────────

    @Test
    @DisplayName("getSnapshotsByBranch: returns only snapshots in given branch")
    void getSnapshotsByBranch() {
        Snapshot featureSnap = Snapshot.builder().snapshotId(2).projectId(10).branch("feature/x").build();
        when(snapshotRepository.findByProjectIdAndBranchOrderByCreatedAtDesc(10, "feature/x"))
                .thenReturn(List.of(featureSnap));

        List<Snapshot> result = versionService.getSnapshotsByBranch(10, "feature/x");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBranch()).isEqualTo("feature/x");
    }

    // ── getBranchesByProject ───────────────────────────────────────────────────

    @Test
    @DisplayName("getBranchesByProject: always includes 'main' even if no snapshots")
    void getBranchesByProject_includesMainDefault() {
        when(snapshotRepository.findDistinctBranchesByProjectId(10)).thenReturn(List.of());
        List<String> branches = versionService.getBranchesByProject(10);
        assertThat(branches).contains("main");
    }

    @Test
    @DisplayName("getBranchesByProject: returns all distinct branches")
    void getBranchesByProject_returnsAll() {
        when(snapshotRepository.findDistinctBranchesByProjectId(10))
                .thenReturn(List.of("main", "develop", "feature/auth"));

        List<String> branches = versionService.getBranchesByProject(10);
        assertThat(branches).containsExactlyInAnyOrder("main", "develop", "feature/auth");
    }

    // ── restoreSnapshotDto ─────────────────────────────────────────────────────

    @Test
    @DisplayName("restoreSnapshotDto: creates a new snapshot and returns DTO with original content")
    void restoreSnapshot_success() {
        when(snapshotRepository.findById(1)).thenReturn(Optional.of(sampleSnapshot));
        when(snapshotRepository.save(any())).thenAnswer(inv -> {
            Snapshot s = inv.getArgument(0);
            s.setSnapshotId(99);
            return s;
        });

        RestoreResponseDto dto = versionService.restoreSnapshotDto(1);

        assertThat(dto.getRestoredFromSnapshotId()).isEqualTo(1);
        assertThat(dto.getContent()).isEqualTo("public class Main {}");
        assertThat(dto.getMessage()).contains("restored");
    }

    @Test
    @DisplayName("restoreSnapshotDto: throws when snapshot not found")
    void restoreSnapshot_notFound() {
        when(snapshotRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> versionService.restoreSnapshotDto(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ── getFileHistoryDto ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getFileHistoryDto: returns mapped DTOs ordered by date desc")
    void getFileHistoryDto() {
        Snapshot s2 = Snapshot.builder().snapshotId(2).fileId(100).projectId(10)
                .branch("main").message("Second").createdAt(LocalDateTime.now()).build();
        when(snapshotRepository.findByFileIdOrderByCreatedAtDesc(100))
                .thenReturn(List.of(s2, sampleSnapshot));

        List<SnapshotResponseDto> history = versionService.getFileHistoryDto(100);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getSnapshotId()).isEqualTo(2);
    }

    // ── tagSnapshot ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("tagSnapshot: updates tag on snapshot")
    void tagSnapshot_success() {
        when(snapshotRepository.findById(1)).thenReturn(Optional.of(sampleSnapshot));
        when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        versionService.tagSnapshot(1, "v1.0.0");

        assertThat(sampleSnapshot.getTag()).isEqualTo("v1.0.0");
        verify(snapshotRepository).save(sampleSnapshot);
    }

    @Test
    @DisplayName("tagSnapshot: throws when snapshot not found")
    void tagSnapshot_notFound() {
        when(snapshotRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> versionService.tagSnapshot(999, "v1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
