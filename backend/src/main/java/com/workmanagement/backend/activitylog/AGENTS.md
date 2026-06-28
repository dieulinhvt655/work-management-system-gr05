# Activity Log Module

Module ghi nhận và tra cứu lịch sử hoạt động. Phục vụ **UC 2.0** và **UC 3.0**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-2.9 | Theo dõi thông tin hoạt động của tổ chức | All actors | FR-2.7 |
| UC-3.10 | Xem lịch sử hoạt động dự án | All actors | FR-3.4 |

## Phạm vi module

- Ghi log hoạt động ở cấp workspace / tổ chức.
- Ghi log hoạt động ở cấp dự án.
- Lọc và tra cứu lịch sử (`ActivityLogFilterRequest`).

## Liên quan module khác

- Các module nghiệp vụ (workspace, project, sprint, task…) gọi service ghi log khi có sự kiện.

## Controllers & DTOs chính

- `ActivityLogController`
- Request: `ActivityLogFilterRequest`
- Response: `ActivityLogResponse`
