# Thuật ngữ (Glossary)

| ID | Thuật ngữ | Ý nghĩa |
|---|---|---|
| 1.1 | Work Management System | Hệ thống hỗ trợ quản lý công việc, quy trình vận hành và phối hợp hoạt động trong doanh nghiệp. |
| 1.2 | Project Management | Hoạt động lập kế hoạch, theo dõi và kiểm soát tiến độ thực hiện dự án. |
| 1.3 | Task Management | Quá trình tạo, phân công, theo dõi và cập nhật trạng thái công việc. |
| 1.4 | Workflow Management | Cơ chế quản lý luồng xử lý và trạng thái công việc theo quy trình xác định. |
| 1.5 | Business Process | Chuỗi các hoạt động nghiệp vụ được thực hiện nhằm đạt mục tiêu vận hành của doanh nghiệp |
| 1.6 | Operation Management | Hoạt động quản lý và điều phối các quy trình vận hành trong tổ chức |
| 1.7 | Collaboration | Quá trình phối hợp và trao đổi giữa các thành viên trong quá trình thực hiện công việc |
| 1.8 | Productivity | Mức độ hiệu quả trong quá trình thực hiện công việc và sử dụng nguồn lực |
| 1.9 | Enterprise | Tổ chức hoặc doanh nghiệp sử dụng hệ thống để phục vụ hoạt động quản trị và vận hành |git checkout -b docs/add-requirements-and-architecture
| 1.10| Organization | Đơn vị tổ chức bao gồm nhiều nhóm làm việc trong doanh nghiệp |
| 1.11| Team | Nhóm người dùng phối hợp thực hiện công việc hoặc dự án chung |
| 1.12| Workspace | Không gian làm việc dùng để quản lý thành viên, dự án và dữ liệu liên quan |
| 1.14| Resource Management | Hoạt động quản lý và phân bổ nguồn lực như nhân sự, thời gian và khối lượng công việc |
| 1.15| Workload | Khối lượng công việc được phân bổ cho cá nhân hoặc nhóm trong một khoảng thời gian |
| 1.16| Performance | Mức độ hoàn thành công việc dựa trên hiệu suất và kết quả thực hiện |
| 1.17| KPI | Chỉ số dùng để đánh giá hiệu quả hoạt động hoặc mức độ hoàn thành mục tiêu |
| 1.18| Dashboard | Giao diện tổng hợp dữ liệu và thông tin vận hành dưới dạng trực quan |
| 2.1 | Project | Một tập hợp công việc được thực hiện nhằm đạt một mục tiêu cụ thể trong khoảng thời gian xác định |
| 2.4 | Sprint | Chu kỳ phát triển ngắn trong mô hình Agile nhằm hoàn thành một nhóm công việc xác định |
| 2.5 | Backlog | Danh sách các công việc, yêu cầu hoặc tính năng cần được thực hiện |
| 2.10| Deadline | Thời hạn cuối cùng cần hoàn thành công việc hoặc dự án |
| 3.1 | Task | Đơn vị công việc cần được thực hiện trong dự án hoặc quy trình vận hành |
| 3.8 | Status | Trạng thái hiện tại của công việc hoặc quy trình xử lý |
| 3.23| Notification | Thông báo được gửi đến người dùng khi có sự kiện hoặc thay đổi liên quan |
| 4.1 | Agile | Phương pháp phát triển phần mềm linh hoạt tập trung vào khả năng thích ứng, cộng tác và cải tiến liên tục |
| 4.2 | Scrum | Framework quản lý và phát triển sản phẩm theo Agile dựa trên Sprint, vai trò và các sự kiện Scrum |
| 4.3 | Kanban | Mô hình quản lý công việc trực quan dựa trên luồng xử lý và giới hạn công việc đang thực hiện |
| 6.5 | Role | Nhóm vai trò đại diện cho chức năng và phạm vi quyền hạn của người dùng |
| 6.8 | RBAC | Mô hình phân quyền dựa trên vai trò của người dùng trong tổ chức hoặc hệ thống |

# Thông số kỹ thuật bổ sung

## Phân tích Actors

| STT | Actor | Vai trò chính |
|---|---|---|
| 1 | Workspace Owner | Người sở hữu và quản trị cao nhất của Workspace, chịu trách nhiệm quản lý tổ chức và toàn bộ hoạt động trong Workspace. |
| 2 | System Admin | Quản trị viên hệ thống, chịu trách nhiệm cấu hình hệ thống, quản lý người dùng và hỗ trợ vận hành hệ thống. |
| 3 | Team Leader | Người quản lý các nhóm làm việc trong tổ chức, chịu trách nhiệm quản lý nhân sự và nguồn lực thuộc phạm vi được giao. |
| 4 | Project Manager | Người quản lý dự án, chịu trách nhiệm lập kế hoạch, điều phối nguồn lực và theo dõi tiến độ thực hiện dự án. |
| 5 | Team Member | Thành viên thực hiện công việc, tham gia dự án và xử lý các nhiệm vụ được giao. |