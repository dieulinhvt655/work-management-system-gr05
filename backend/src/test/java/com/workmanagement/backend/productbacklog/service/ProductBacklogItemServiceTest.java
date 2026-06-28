package com.workmanagement.backend.productbacklog.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.PbiStatus;
import com.workmanagement.backend.common.enums.PbiType;
import com.workmanagement.backend.common.enums.PriorityLevel;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.productbacklog.dto.request.CreateProductBacklogItemRequest;
import com.workmanagement.backend.productbacklog.dto.request.UpdateProductBacklogItemRequest;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogItemResponse;
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import com.workmanagement.backend.productbacklog.entity.ProductBacklogItem;
import com.workmanagement.backend.productbacklog.mapper.ProductBacklogItemMapper;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogItemRepository;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductBacklogItemServiceTest {

    @Mock
    private ProductBacklogItemRepository productBacklogItemRepository;
    @Mock
    private ProductBacklogItemMapper productBacklogItemMapper;
    @Mock
    private ProductBacklogService productBacklogService;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProductBacklogItemService productBacklogItemService;

    private Project project;
    private ProductBacklog backlog;
    private ProductBacklogItem item;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        Team team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        project = Project.builder().id(30L).team(team).name("Alpha").status(ProjectStatus.ACTIVE).build();
        backlog = ProductBacklog.builder().id(50L).project(project).name("Alpha Backlog").build();
        item = ProductBacklogItem.builder()
                .id(60L)
                .backlog(backlog)
                .title("Login feature")
                .type(PbiType.FEATURE)
                .priority(PriorityLevel.HIGH)
                .status(PbiStatus.NEW)
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_shouldSaveNewPbi() {
        CreateProductBacklogItemRequest request = new CreateProductBacklogItemRequest();
        request.setTitle("Login feature");
        request.setType(PbiType.FEATURE);
        request.setPriority(PriorityLevel.HIGH);

        ProductBacklogItemResponse response = ProductBacklogItemResponse.builder()
                .id(60L)
                .title("Login feature")
                .status(PbiStatus.NEW)
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(productBacklogService).verifyIsProjectManager(project);
        doNothing().when(productBacklogService).ensureProjectAcceptsBacklogChanges(project);
        when(productBacklogService.getOrCreateBacklog(project)).thenReturn(backlog);
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.empty());
        when(productBacklogItemRepository.save(any())).thenReturn(item);
        when(productBacklogItemMapper.toResponse(item)).thenReturn(response);

        ProductBacklogItemResponse result = productBacklogItemService.create(10L, 20L, 30L, request);

        assertThat(result.getTitle()).isEqualTo("Login feature");
        verify(productBacklogItemRepository).save(any(ProductBacklogItem.class));
    }

    @Test
    void create_shouldRejectNonProjectManager() {
        CreateProductBacklogItemRequest request = new CreateProductBacklogItemRequest();
        request.setTitle("Login feature");

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doThrow(new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED, "denied"))
                .when(productBacklogService).verifyIsProjectManager(project);

        assertThatThrownBy(() -> productBacklogItemService.create(10L, 20L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
    }

    @Test
    void findById_shouldReturnItem() {
        ProductBacklogItemResponse response = ProductBacklogItemResponse.builder().id(60L).title("Login feature").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(item));
        when(productBacklogItemMapper.toResponse(item)).thenReturn(response);

        ProductBacklogItemResponse result = productBacklogItemService.findById(10L, 20L, 30L, 60L);

        assertThat(result.getId()).isEqualTo(60L);
    }

    @Test
    void findAll_shouldReturnPagedItems() {
        ProductBacklogItemResponse response = ProductBacklogItemResponse.builder().id(60L).build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(productBacklogService.getOrCreateBacklog(project)).thenReturn(backlog);
        when(productBacklogItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));
        when(productBacklogItemMapper.toResponse(item)).thenReturn(response);

        PageResponse<ProductBacklogItemResponse> result = productBacklogItemService.findAll(
                10L, 20L, 30L, 0, 20, "login", PbiStatus.NEW, PbiType.FEATURE, PriorityLevel.HIGH
        );

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void update_shouldUpdateEditableItem() {
        UpdateProductBacklogItemRequest request = new UpdateProductBacklogItemRequest();
        request.setTitle("Updated title");

        ProductBacklogItemResponse response = ProductBacklogItemResponse.builder().id(60L).title("Updated title").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(productBacklogService).verifyIsProjectManager(project);
        doNothing().when(productBacklogService).ensureProjectAcceptsBacklogChanges(project);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(item));
        when(productBacklogItemRepository.save(item)).thenReturn(item);
        when(productBacklogItemMapper.toResponse(item)).thenReturn(response);

        ProductBacklogItemResponse result = productBacklogItemService.update(10L, 20L, 30L, 60L, request);

        assertThat(result.getTitle()).isEqualTo("Updated title");
    }

    @Test
    void update_shouldRejectInSprintItem() {
        item.setStatus(PbiStatus.IN_SPRINT);
        UpdateProductBacklogItemRequest request = new UpdateProductBacklogItemRequest();
        request.setTitle("Updated title");

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(productBacklogService).verifyIsProjectManager(project);
        doNothing().when(productBacklogService).ensureProjectAcceptsBacklogChanges(project);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> productBacklogItemService.update(10L, 20L, 30L, 60L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PBI_INVALID_STATUS);
    }

    @Test
    void delete_shouldDeleteNewItemWithoutTasks() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(productBacklogService).verifyIsProjectManager(project);
        doNothing().when(productBacklogService).ensureProjectAcceptsBacklogChanges(project);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(item));
        when(productBacklogItemRepository.countTasksByPbiId(60L)).thenReturn(0L);

        productBacklogItemService.delete(10L, 20L, 30L, 60L);

        verify(productBacklogItemRepository).delete(item);
    }

    @Test
    void delete_shouldRejectItemWithTasks() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(productBacklogService).verifyIsProjectManager(project);
        doNothing().when(productBacklogService).ensureProjectAcceptsBacklogChanges(project);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(item));
        when(productBacklogItemRepository.countTasksByPbiId(60L)).thenReturn(2L);

        assertThatThrownBy(() -> productBacklogItemService.delete(10L, 20L, 30L, 60L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PBI_CANNOT_DELETE);
    }

    @Test
    void markReady_shouldChangeStatusToReady() {
        ProductBacklogItemResponse response = ProductBacklogItemResponse.builder()
                .id(60L)
                .status(PbiStatus.READY)
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(productBacklogService).verifyIsProjectManager(project);
        doNothing().when(productBacklogService).ensureProjectAcceptsBacklogChanges(project);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(item));
        when(productBacklogItemRepository.save(item)).thenReturn(item);
        when(productBacklogItemMapper.toResponse(item)).thenReturn(response);

        ProductBacklogItemResponse result = productBacklogItemService.markReady(10L, 20L, 30L, 60L);

        assertThat(result.getStatus()).isEqualTo(PbiStatus.READY);
        assertThat(item.getStatus()).isEqualTo(PbiStatus.READY);
    }

    @Test
    void markReady_shouldRejectCompletedItem() {
        item.setStatus(PbiStatus.COMPLETED);

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(productBacklogService).verifyIsProjectManager(project);
        doNothing().when(productBacklogService).ensureProjectAcceptsBacklogChanges(project);
        when(productBacklogItemRepository.findByIdAndBacklog_Project_Id(60L, 30L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> productBacklogItemService.markReady(10L, 20L, 30L, 60L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PBI_INVALID_STATUS);
    }

}
