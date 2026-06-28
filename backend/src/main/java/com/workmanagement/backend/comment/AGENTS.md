# Comment Module

Module quản lý bình luận và trao đổi. Thuộc nhóm **UC 6.0 – Quản lý Cộng tác và trao đổi**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-6.1 | Tạo bình luận trao đổi | PM, Team Member | FR-6.1 |
| UC-6.2 | Chỉnh sửa hoặc xóa bình luận | PM, Team Member | FR-6.1 |
| UC-6.4 | Xem trao đổi trong Task | PM, Team Member | FR-6.3 |

## Phạm vi module

- Tạo bình luận gắn với task (hoặc entity liên quan).
- Chỉnh sửa / xóa bình luận (chỉ tác giả hoặc có quyền).
- Liệt kê thread trao đổi theo task.

## Liên quan module khác

- Đính kèm tệp vào trao đổi (UC-6.3) → `attachment/`
- Task là ngữ cảnh chính → `task/`

## Controllers & DTOs chính

- `CommentController`
- Request: `CreateCommentRequest`, `UpdateCommentRequest`
- Response: `CommentResponse`
