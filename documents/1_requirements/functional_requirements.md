# Mô hình hoá chức năng

## 1. Các yêu cầu chức năng (Functional Requirements)

| ID | Yêu cầu chức năng | Mô tả | Actor |
|---|---|---|---|
| **FR-1** | **Quản lý người dùng và phân quyền** | | |
| FR-1.1 | Quản lý tài khoản người dùng | Tạo, cập nhật, khóa/mở khóa tài khoản | Workspace Owner, System Admin |
| FR-1.2 | Xác thực và quản lý phiên đăng nhập | Đăng nhập, đăng xuất, xác thực | All actors |
| FR-1.3 | Khôi phục mật khẩu | Yêu cầu khôi phục mật khẩu | Guest User |
| FR-1.4 | Quản lý hồ sơ cá nhân | Xem, cập nhật thông tin cá nhân | All actors |
| FR-1.5 | Quản lý vai trò và phân quyền | Gán vai trò, kiểm soát quyền truy cập | Workspace Owner, System Admin |
| **FR-2** | **Quản lý Workspace / tổ chức** | | |
| FR-2.1 | Tạo Workspace và thiết lập tổ chức | Tạo Workspace đại diện cho doanh nghiệp | Workspace Owner, System Admin |
| FR-2.2 | Quản lý thông tin Workspace | Cập nhật tên, mô tả, logo, thiết lập | Workspace Owner, System Admin |
| FR-2.3 | Quản lý cơ cấu tổ chức | Tạo, cập nhật, giải thể nhóm làm việc | Workspace Owner, System Admin |
| FR-2.4 | Quản lý thành viên Workspace | Thêm, cập nhật, loại bỏ thành viên | Workspace Owner, System Admin |
| FR-2.5 | Phân công quản lý nhóm | Gán Team Leader cho nhóm | Workspace Owner, System Admin |
| FR-2.6 | Phân quyền trong phạm vi tổ chức | Phân bổ thành viên vào nhóm/dự án | Team Leader |
| FR-2.7 | Theo dõi hoạt động tổ chức | Theo dõi thông tin hoạt động | Workspace Owner, System Admin, Team Leader |
| FR-2.8 | Đóng / vô hiệu hóa Workspace | Đóng, vô hiệu hóa khi ngừng sử dụng | Workspace Owner, System Admin |
| **FR-3** | **Quản lý dự án** | | |
| FR-3.1 | Quản lý thông tin dự án | Tạo mới, kích hoạt, quản lý thông tin | Team Leader, Project Manager |
| FR-3.2 | Quản lý thành viên và vai trò | Thêm, loại bỏ, gán vai trò | Team Leader |
| FR-3.3 | Quản lý tài liệu dự án | Tải lên, cập nhật, quản lý tài liệu | Team Leader, Project Manager |
| FR-3.4 | Tra cứu thông báo và lịch sử | Nhận thông báo, tra cứu lịch sử | Team Leader, Project Manager, Team Member |
| FR-3.5 | Kết thúc và lưu trữ dự án | Kết thúc, lưu trữ dự án | Team Leader, Project Manager |
| **FR-4** | **Quản lý Product Backlog và Công việc** | | |
| FR-4.1 | Quản lý Product Backlog | Tạo, cập nhật, xóa PBI | PM |
| FR-4.2 | Tra cứu Product Backlog | Tra cứu, lọc PBI | PM, Team Leader |
| FR-4.3 | Quản lý Task chuẩn bị | Tạo, cập nhật, xóa task chuẩn bị | PM |
| FR-4.4 | Phân công nguồn lực dự kiến | Chỉ định thành viên dự kiến | PM |
| FR-4.5 | Xác nhận PBI sẵn sàng | Chuyển PBI sang trạng thái Ready | PM |
| **FR-5** | **Quản lý Sprint / Workflow và quy trình vận hành** | | |
| FR-5.1 | Quản lý Sprint | Tạo, cập nhật, kết thúc Sprint | PM |
| FR-5.2 | Quản lý PBI trong Sprint | Đưa PBI vào Sprint, cập nhật trạng thái | PM |
| FR-5.3 | Quản lý công việc và phân công | Quản lý task, xác nhận phân công | PM |
| FR-5.4 | Quản lý Workflow công việc | Kiểm soát luồng xử lý (To Do, In Progress...) | PM |
| FR-5.5 | Theo dõi tiến độ Sprint | Giám sát, đánh giá, kiểm tra kết quả | PM, Team Leader |
| FR-5.6 | Quản lý kết quả và lịch sử Sprint| Tổng kết, lưu trữ thông tin | PM |
| **FR-6** | **Quản lý Cộng tác và trao đổi** | | |
| FR-6.1 | Quản lý trao đổi trên Task | Tạo, phản hồi, xóa bình luận | PM, Team Member |
| FR-6.2 | Quản lý tệp đính kèm | Đính kèm, xem, tải tệp | PM, Team Member |
| FR-6.3 | Tra cứu lịch sử trao đổi | Xem lịch sử bình luận, tệp | PM, Team Member |
| **FR-7** | **Quản lý Dashboard** | | |
| FR-7.1 | Quản lý Dashboard | Xem số liệu, biểu đồ tổng hợp theo quyền | All actors |

## 2. Danh sách Use-case (Phân rã)
- **UC-1.0**: Quản lý người dùng và phân quyền (Đăng nhập, Đăng xuất, Khôi phục mật khẩu, Quản lý hồ sơ, Tạo tài khoản, Cập nhật vai trò...)
- **UC-2.0**: Quản lý Workspace / tổ chức (Tạo thiết lập Workspace, Quản lý nhóm, Phân bổ thành viên...)
- **UC-3.0**: Quản lý Dự án (Tạo dự án mới, Quản lý thành viên, Kích hoạt dự án, Lưu trữ dự án...)
- **UC-4.0**: Quản lý Product Backlog và Công việc (Tạo PBI, Phân rã Task, Gán thành viên dự kiến...)
- **UC-5.0**: Quản lý Sprint / Workflow (Quản lý Sprint, Thực hiện công việc, Phê duyệt công việc...)
- **UC-6.0**: Quản lý Cộng tác và trao đổi (Tạo bình luận, Đính kèm tệp...)
- **UC-7.0**: Quản lý Dashboard (Xem Dashboard tổng quan, Team, Dự án, Cá nhân)