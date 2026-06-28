# Task Module

Module quản lý Task và Workflow. Phục vụ **UC 4.0** và **UC 5.0**.

## Use Cases — Product Backlog (UC 4.0)

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-4.5 | Phân rã Product Backlog Item thành Task | PM | FR-4.3 |
| UC-4.6 | Cập nhật Task chuẩn bị | PM | FR-4.3 |
| UC-4.7 | Xóa Task chuẩn bị | PM | FR-4.3 |
| UC-4.8 | Gán thành viên dự kiến cho Task | PM | FR-4.4 |

## Use Cases — Sprint / Workflow (UC 5.0)

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-5.3 | Quản lý công việc trong Sprint | PM | FR-5.3 |
| UC-5.4 | Rà soát và xác nhận phân công công việc | PM | FR-5.3 |
| UC-5.5 | Thực hiện công việc trong Sprint | PM | FR-5.4 |
| UC-5.7 | Kiểm tra và phê duyệt công việc | PM | FR-5.5 |

## Phạm vi module

- Task chuẩn bị (backlog refinement): tạo, sửa, xóa từ PBI.
- Gán người thực hiện dự kiến.
- Quản lý task trong sprint: phân công, thực hiện, phê duyệt.
- Workflow states và transitions (`WorkflowStateController`, `WorkflowTransitionController`).

## Liên quan module khác

- PBI nguồn → `productbacklog/`
- Sprint chứa task → `sprint/`
- Bình luận trên task (UC-6.4) → `comment/`

## Controllers chính

- `TaskController`
- `WorkflowStateController`, `WorkflowTransitionController`
