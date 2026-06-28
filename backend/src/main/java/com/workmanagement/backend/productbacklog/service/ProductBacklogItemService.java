package com.workmanagement.backend.productbacklog.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.common.constant.ErrorCode;
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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductBacklogItemService {

    private static final Set<PbiStatus> DELETABLE_STATUSES = Set.of(PbiStatus.NEW, PbiStatus.ON_HOLD);
    private static final Set<PbiStatus> READY_SOURCE_STATUSES = Set.of(PbiStatus.NEW, PbiStatus.ON_HOLD);
    private static final Set<PbiStatus> EDITABLE_STATUSES = Set.of(
            PbiStatus.NEW, PbiStatus.READY, PbiStatus.ON_HOLD
    );

    private final ProductBacklogItemRepository productBacklogItemRepository;
    private final ProductBacklogItemMapper productBacklogItemMapper;
    private final ProductBacklogService productBacklogService;
    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;
    private final ActivityLogService activityLogService;

    /** UC-4.1 — Tạo Product Backlog Item */
    @Transactional
    @PreAuthorize("hasAuthority('backlog:create')")
    public ProductBacklogItemResponse create(
            Long workspaceId,
            Long teamId,
            Long projectId,
            CreateProductBacklogItemRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        productBacklogService.verifyIsProjectManager(project);
        productBacklogService.ensureProjectAcceptsBacklogChanges(project);

        ProductBacklog backlog = productBacklogService.getOrCreateBacklog(project);
        ProjectMember proposer = resolveProposer(project, request.getProposerMemberId());

        ProductBacklogItem item = ProductBacklogItem.builder()
                .backlog(backlog)
                .proposerMember(proposer)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType() : PbiType.FEATURE)
                .priority(request.getPriority() != null ? request.getPriority() : PriorityLevel.MEDIUM)
                .status(PbiStatus.NEW)
                .desiredDueDate(request.getDesiredDueDate())
                .build();
        item = productBacklogItemRepository.save(item);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PBI_CREATED,
                ActivityLogAction.TARGET_PBI,
                item.getId(),
                item.getTitle(),
                project
        );

        return productBacklogItemMapper.toResponse(item);
    }

    /** UC-4.4 — Tra cứu chi tiết PBI */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('backlog:read')")
    public ProductBacklogItemResponse findById(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        ProductBacklogItem item = getItem(projectId, itemId);
        return productBacklogItemMapper.toResponse(item);
    }

    /** UC-4.4, UC-4.10 — Danh sách / tìm kiếm / lọc PBI */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('backlog:read')")
    public PageResponse<ProductBacklogItemResponse> findAll(
            Long workspaceId,
            Long teamId,
            Long projectId,
            int page,
            int size,
            String keyword,
            PbiStatus status,
            PbiType type,
            PriorityLevel priority
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        projectService.verifyProjectAccess(project);

        ProductBacklog backlog = productBacklogService.getOrCreateBacklog(project);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<ProductBacklogItem> spec = buildSearchSpec(backlog.getId(), keyword, status, type, priority);
        Page<ProductBacklogItem> result = productBacklogItemRepository.findAll(spec, pageable);

        return PageResponse.<ProductBacklogItemResponse>builder()
                .items(result.getContent().stream().map(productBacklogItemMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** UC-4.2 — Cập nhật Product Backlog Item */
    @Transactional
    @PreAuthorize("hasAuthority('backlog:update')")
    public ProductBacklogItemResponse update(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId,
            UpdateProductBacklogItemRequest request
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        productBacklogService.verifyIsProjectManager(project);
        productBacklogService.ensureProjectAcceptsBacklogChanges(project);

        ProductBacklogItem item = getItem(projectId, itemId);
        ensureItemEditable(item);

        if (StringUtils.hasText(request.getTitle())) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            item.setType(request.getType());
        }
        if (request.getPriority() != null) {
            item.setPriority(request.getPriority());
        }
        if (request.getDesiredDueDate() != null) {
            item.setDesiredDueDate(request.getDesiredDueDate());
        }

        item = productBacklogItemRepository.save(item);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PBI_UPDATED,
                ActivityLogAction.TARGET_PBI,
                item.getId(),
                item.getTitle(),
                project
        );

        return productBacklogItemMapper.toResponse(item);
    }

    /** UC-4.3 — Xóa Product Backlog Item */
    @Transactional
    @PreAuthorize("hasAuthority('backlog:delete')")
    public void delete(Long workspaceId, Long teamId, Long projectId, Long itemId) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        productBacklogService.verifyIsProjectManager(project);
        productBacklogService.ensureProjectAcceptsBacklogChanges(project);

        ProductBacklogItem item = getItem(projectId, itemId);

        if (!DELETABLE_STATUSES.contains(item.getStatus())) {
            throw new BusinessException(
                    ErrorCode.PBI_CANNOT_DELETE,
                    "Chỉ PBI ở trạng thái mới hoặc tạm dừng mới được xóa"
            );
        }
        if (productBacklogItemRepository.countTasksByPbiId(item.getId()) > 0) {
            throw new BusinessException(
                    ErrorCode.PBI_CANNOT_DELETE,
                    "Không thể xóa PBI đã có task liên kết"
            );
        }

        String title = item.getTitle();
        productBacklogItemRepository.delete(item);

        activityLogService.recordProjectEvent(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PBI_DELETED,
                ActivityLogAction.TARGET_PBI,
                itemId,
                title,
                project
        );
    }

    /** UC-4.9 — Xác nhận PBI sẵn sàng triển khai */
    @Transactional
    @PreAuthorize("hasAuthority('backlog:update')")
    public ProductBacklogItemResponse markReady(
            Long workspaceId,
            Long teamId,
            Long projectId,
            Long itemId
    ) {
        Project project = projectService.getProject(workspaceId, teamId, projectId);
        productBacklogService.verifyIsProjectManager(project);
        productBacklogService.ensureProjectAcceptsBacklogChanges(project);

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dự án phải đang hoạt động để xác nhận PBI sẵn sàng");
        }

        ProductBacklogItem item = getItem(projectId, itemId);

        if (!READY_SOURCE_STATUSES.contains(item.getStatus())) {
            throw new BusinessException(
                    ErrorCode.PBI_INVALID_STATUS,
                    "Chỉ PBI mới hoặc tạm dừng mới được chuyển sang sẵn sàng triển khai"
            );
        }

        PbiStatus oldStatus = item.getStatus();
        item.setStatus(PbiStatus.READY);
        item = productBacklogItemRepository.save(item);

        activityLogService.record(
                SecurityUtils.getCurrentUserId(),
                ActivityLogAction.PBI_MARKED_READY,
                ActivityLogAction.TARGET_PBI,
                item.getId(),
                oldStatus.getValue(),
                PbiStatus.READY.getValue(),
                project
        );

        return productBacklogItemMapper.toResponse(item);
    }

    private ProductBacklogItem getItem(Long projectId, Long itemId) {
        return productBacklogItemRepository.findByIdAndBacklog_Project_Id(itemId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PBI_NOT_FOUND, "Không tìm thấy PBI"));
    }

    private void ensureItemEditable(ProductBacklogItem item) {
        if (!EDITABLE_STATUSES.contains(item.getStatus())) {
            throw new BusinessException(
                    ErrorCode.PBI_INVALID_STATUS,
                    "PBI đang trong sprint hoặc đã hoàn thành, không thể cập nhật"
            );
        }
    }

    private ProjectMember resolveProposer(Project project, Long proposerMemberId) {
        if (proposerMemberId != null) {
            return projectMemberRepository.findByIdAndProjectId(proposerMemberId, project.getId())
                    .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.PROJECT_MEMBER_NOT_FOUND,
                            "Người đề xuất phải là thành viên active của dự án"
                    ));
        }

        return projectMemberRepository
                .findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                        project.getId(),
                        SecurityUtils.getCurrentUserId(),
                        MemberStatus.ACTIVE
                )
                .orElse(null);
    }

    private Specification<ProductBacklogItem> buildSearchSpec(
            Long backlogId,
            String keyword,
            PbiStatus status,
            PbiType type,
            PriorityLevel priority
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("backlog").get("id"), backlogId));

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

}
