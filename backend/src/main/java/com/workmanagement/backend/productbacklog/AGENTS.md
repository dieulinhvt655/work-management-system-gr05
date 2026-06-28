# Product Backlog Module

Module quản lý Product Backlog Item (PBI). Thuộc nhóm **UC 4.0 – Quản lý Product Backlog và Công việc**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-4.1 | Tạo Product Backlog Item | PM | FR-4.1 |
| UC-4.2 | Cập nhật Product Backlog Item | PM | FR-4.1 |
| UC-4.3 | Xóa Product Backlog Item | PM | FR-4.1 |
| UC-4.4 | Tra cứu Product Backlog Item | PM / Team Leader | FR-4.2 |
| UC-4.9 | Xác nhận PBI sẵn sàng triển khai | PM | FR-4.5 |
| UC-4.10 | Tìm kiếm và lọc Product Backlog Item | PM / Team Leader | FR-4.2 |

## Phạm vi module

- CRUD Product Backlog Item.
- Tra cứu, tìm kiếm và lọc PBI theo tiêu chí.
- Chuyển trạng thái PBI sang "sẵn sàng triển khai" (ready).

## Liên quan module khác

- Phân rã PBI thành Task (UC-4.5 → UC-4.8) → `task/`
- Quản lý PBI trong Sprint (UC-5.2) → `sprint/`

## Controllers & DTOs chính

- `ProductBacklogController`, `ProductBacklogItemController`
- Request: `CreateProductBacklogItemRequest`, `UpdateProductBacklogItemRequest`
