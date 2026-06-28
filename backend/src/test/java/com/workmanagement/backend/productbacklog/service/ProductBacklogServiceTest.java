package com.workmanagement.backend.productbacklog.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.productbacklog.dto.request.UpdateProductBacklogRequest;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogResponse;
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import com.workmanagement.backend.productbacklog.mapper.ProductBacklogMapper;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogRepository;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.workspace.entity.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductBacklogServiceTest {

    @Mock
    private ProductBacklogRepository productBacklogRepository;
    @Mock
    private ProductBacklogMapper productBacklogMapper;
    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProductBacklogService productBacklogService;

    private Project project;
    private ProductBacklog backlog;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        Team team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        project = Project.builder().id(30L).team(team).name("Alpha").status(ProjectStatus.ACTIVE).build();
        backlog = ProductBacklog.builder().id(50L).project(project).name("Alpha Backlog").build();
    }

    @Test
    void getOrCreateBacklog_shouldReturnExisting() {
        when(productBacklogRepository.findByProjectId(30L)).thenReturn(Optional.of(backlog));

        ProductBacklog result = productBacklogService.getOrCreateBacklog(project);

        assertThat(result.getId()).isEqualTo(50L);
    }

    @Test
    void getOrCreateBacklog_shouldCreateWhenMissing() {
        when(productBacklogRepository.findByProjectId(30L)).thenReturn(Optional.empty());
        when(productBacklogRepository.save(any())).thenReturn(backlog);

        ProductBacklog result = productBacklogService.getOrCreateBacklog(project);

        assertThat(result.getName()).isEqualTo("Alpha Backlog");
        verify(productBacklogRepository).save(any(ProductBacklog.class));
    }

    @Test
    void findByProject_shouldReturnBacklog() {
        ProductBacklogResponse response = ProductBacklogResponse.builder().id(50L).name("Alpha Backlog").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(productBacklogRepository.findByProjectId(30L)).thenReturn(Optional.of(backlog));
        when(productBacklogMapper.toResponse(backlog)).thenReturn(response);

        ProductBacklogResponse result = productBacklogService.findByProject(10L, 20L, 30L);

        assertThat(result.getName()).isEqualTo("Alpha Backlog");
    }

    @Test
    void findByProject_shouldThrowWhenMissing() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(productBacklogRepository.findByProjectId(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productBacklogService.findByProject(10L, 20L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BACKLOG_NOT_FOUND);
    }

    @Test
    void update_shouldUpdateBacklog() {
        UpdateProductBacklogRequest request = new UpdateProductBacklogRequest();
        request.setName("Updated Backlog");

        ProductBacklogResponse response = ProductBacklogResponse.builder().id(50L).name("Updated Backlog").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(true);
        when(productBacklogRepository.findByProjectId(30L)).thenReturn(Optional.of(backlog));
        when(productBacklogRepository.save(backlog)).thenReturn(backlog);
        when(productBacklogMapper.toResponse(backlog)).thenReturn(response);

        ProductBacklogResponse result = productBacklogService.update(10L, 20L, 30L, request);

        assertThat(result.getName()).isEqualTo("Updated Backlog");
    }

    @Test
    void update_shouldRejectNonProjectManager() {
        UpdateProductBacklogRequest request = new UpdateProductBacklogRequest();
        request.setName("Updated Backlog");

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        when(projectService.isActiveProjectManager(project)).thenReturn(false);

        assertThatThrownBy(() -> productBacklogService.update(10L, 20L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
    }

}
