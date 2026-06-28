# Sprint Module

Module quản lý Sprint. Thuộc nhóm **UC 5.0 – Quản lý Sprint / Workflow và quy trình vận hành**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-5.1 | Quản lý Sprint | PM | FR-5.1 |
| UC-5.2 | Quản lý PBI trong Sprint | PM | FR-5.2 |
| UC-5.6 | Theo dõi tiến độ Sprint | PM / Team Leader | FR-5.5 |
| UC-5.8 | Tổng kết Sprint | PM | FR-5.6 |
| UC-5.9 | Tra cứu lịch sử Sprint | PM | FR-5.6 |

## Phạm vi module

- Tạo, cập nhật, đóng sprint.
- Thêm / gỡ PBI khỏi sprint.
- Theo dõi tiến độ (burndown, trạng thái task).
- Tổng kết sprint và lưu lịch sử.

## Liên quan module khác

- Task trong sprint (UC-5.3 → UC-5.5, UC-5.7) → `task/`
- PBI → `productbacklog/`
- Dự án chứa sprint → `project/`

## Controllers chính

- `SprintController`
