# Key Abstraction

Trong quá trình phân tích hệ thống Work Management System, việc xác định các Key Abstractions nhằm làm rõ những khái niệm nghiệp vụ quan trọng cần được quản lý trong hệ thống. Các Key Abstractions được rút ra từ mô tả nghiệp vụ, danh sách yêu cầu chức năng và các use case đã phân tích. Đây là cơ sở để xây dựng Domain Model, giúp biểu diễn các đối tượng nghiệp vụ chính, thông tin cần quản lý, trạng thái và mối quan hệ giữa các đối tượng. Các Key Abstractions không được xác định theo hướng thiết kế cơ sở dữ liệu, mà tập trung vào các khái niệm nghiệp vụ cốt lõi.

## Danh sách Key Abstraction

| Key Abstraction | Vai trò trong hệ thống |
|---|---|
| **User** | Đại diện cho tài khoản người dùng sử dụng hệ thống. User có thể tham gia Workspace, thuộc Team, tham gia Project, thực hiện Task. Trạng thái: Active, Inactive, Deleted. |
| **Workspace** | Đại diện cho không gian làm việc cấp cao nhất của hệ thống, dùng để quản lý thành viên, team/phòng ban và các dự án. Trạng thái: Active, Suspended, Disabled. |
| **Team** | Đại diện cho nhóm làm việc hoặc phòng ban trong Workspace. Dùng để tổ chức nhân sự và quản lý các dự án thuộc phạm vi nhóm/phòng ban. Trạng thái: Active, Inactive, Dissolved. |
| **Project** | Đại diện cho một dự án cần được quản lý trong hệ thống. Phạm vi chính để quản lý thành viên, backlog, sprint, task, tài liệu và tiến độ. Trạng thái: Active, Completed, Archived. |
| **Product Backlog** | Đại diện cho danh sách tập trung các yêu cầu, tính năng, lỗi hoặc hạng mục công việc cần thực hiện trong một dự án. |
| **Product Backlog Item (PBI)** | Đại diện cho một yêu cầu, tính năng, lỗi hoặc hạng mục công việc cần được phân tích, ưu tiên và chuyển thành Task để thực hiện. Trạng thái: New, Ready, In Sprint, Completed, On Hold. |
| **Sprint** | Đại diện cho một chu kỳ triển khai công việc có thời gian xác định trong dự án. Trạng thái: Planning, Active, Completed, Canceled. |
| **Task** | Đại diện cho công việc cụ thể được phân rã từ PBI và được giao cho thành viên thực hiện. Trạng thái: To Do, In Progress, Review, Done, Reopen. |
| **Comment** | Đại diện cho nội dung trao đổi, phản hồi giữa các thành viên trong phạm vi Task. Trạng thái: Created, Edited, Deleted. |
| **Attachment** | Đại diện cho các tệp đính kèm được sử dụng trong dự án, task hoặc comment. Trạng thái: Uploaded, Available, Deleted. |
| **Notification** | Đại diện cho thông báo hệ thống gửi tới người dùng khi có sự kiện phát sinh. Trạng thái: Unread, Read. |
| **Activity Log** | Đại diện cho nhật ký ghi nhận các hoạt động quan trọng trong hệ thống. Phục vụ truy vết, kiểm tra lịch sử. Trạng thái: Recorded. |

## Phân nhóm Key Abstraction
*   **Nhóm quản lý tổ chức:** User, Workspace, Team. Cơ sở để xác định người dùng thuộc tổ chức, nhóm nào.
*   **Nhóm quản lý dự án:** Project. Đóng vai trò trung tâm trong quá trình triển khai dự án.
*   **Nhóm quản lý yêu cầu và công việc:** Product Backlog Item, Sprint, Task. Phản ánh quá trình chuyển đổi từ yêu cầu nghiệp vụ thành công việc cụ thể.
*   **Nhóm cộng tác và tài liệu:** Comment, Attachment. Hỗ trợ quá trình trao đổi thông tin và chia sẻ tài liệu.
*   **Nhóm thông báo và truy vết:** Notification, Activity Log. Gửi thông báo sự kiện và ghi nhận lịch sử hoạt động.