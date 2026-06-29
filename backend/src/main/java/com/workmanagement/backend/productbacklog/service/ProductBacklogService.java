package com.workmanagement.backend.productbacklog.service;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.productbacklog.dto.request.UpdateProductBacklogRequest;
import com.workmanagement.backend.productbacklog.dto.response.ProductBacklogResponse;
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import com.workmanagement.backend.productbacklog.mapper.ProductBacklogMapper;
import com.workmanagement.backend.productbacklog.repository.ProductBacklogRepository;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductBacklogService {

    private final ProductBacklogRepository productBacklogRepository;
    private final ProductBacklogMapper productBacklogMapper;
    private final ProjectService projectService;

    /** Hỗ trợ UC-4.1 — Lấy hoặc tạo backlog mặc định cho dự án */
    @Transactional
    public ProductBacklog getOrCreateBacklog(Project project) {
        return productBacklogRepository.findByProjectId(project.getId())
                .orElseGet(() -> productBacklogRepository.save(ProductBacklog.builder()
                        .project(project)
                        .name(project.getName() + " Backlog")
                        .description("Product backlog của dự án " + project.getName())
                        .build()));
    }

    /** UC-4.4 — Xem thông tin product backlog của dự án */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('backlog:read')")
    public ProductBacklogResponse findByProject(Long workspaceId, Long teamId, Long projectId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        ProductBacklog backlog = productBacklogRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BACKLOG_NOT_FOUND,
                        "Chưa có product backlog cho dự án này"
                ));

        return productBacklogMapper.toResponse(backlog);
    }

    /** UC-4.2 — Cập nhật thông tin product backlog */
    @Transactional
    @PreAuthorize("hasAuthority('backlog:update')")
    public ProductBacklogResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            UpdateProductBacklogRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        verifyIsProjectManager(project);
        ensureProjectAcceptsBacklogChanges(project);

        ProductBacklog backlog = getBacklogForProject(projectId);

        if (StringUtils.hasText(request.getName())) {
            backlog.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            backlog.setDescription(request.getDescription());
        }

        backlog = productBacklogRepository.save(backlog);
        return productBacklogMapper.toResponse(backlog);
    }

    /** Hỗ trợ UC-4.x — Tra cứu backlog theo dự án */
    ProductBacklog getBacklogForProject(Long projectId) {
        return productBacklogRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BACKLOG_NOT_FOUND,
                        "Không tìm thấy product backlog"
                ));
    }

    /** Hỗ trợ UC-4.x — Chặn thao tác khi dự án đã kết thúc hoặc lưu trữ */
    void ensureProjectAcceptsBacklogChanges(Project project) {
        if (project.getStatus() == ProjectStatus.ARCHIVED || project.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án đã kết thúc hoặc lưu trữ");
        }
    }

    /** Hỗ trợ UC-4.x — Chỉ Project Manager được cập nhật backlog */
    void verifyIsProjectManager(Project project) {
        if (!projectService.isActiveProjectManager(project)) {
            throw new BusinessException(
                    ErrorCode.PROJECT_ACCESS_DENIED,
                    "Chỉ Project Manager mới được thực hiện thao tác này"
            );
        }
    }

}
