# Attachment Module

Module quản lý tệp đính kèm. Phục vụ **UC 3.0** và **UC 6.0**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-3.8 | Quản lý tài liệu dự án | Team Leader, Project Manager | FR-3.3 |
| UC-3.9 | Xem danh sách tài liệu dự án | All actors | FR-3.3 |
| UC-6.3 | Đính kèm tệp vào trao đổi | PM, Team Member | FR-6.2 |

## Phạm vi module

- Upload, cập nhật, xóa tệp đính kèm.
- Liên kết tệp với dự án hoặc bình luận / trao đổi.
- Liệt kê tài liệu theo ngữ cảnh (dự án, task, comment).

## Liên quan module khác

- Dự án → `project/`
- Bình luận / trao đổi → `comment/`

## Controllers & DTOs chính

- `AttachmentController`
- Request: `UploadAttachmentRequest`
- Response: `AttachmentResponse`
