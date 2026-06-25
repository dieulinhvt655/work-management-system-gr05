# Đặc tả các Use-case

## UC-1.0: Quản lý người dùng và phân quyền
- **UC-1.1 Đăng nhập**: Cho phép Guest User đăng nhập vào hệ thống bằng tài khoản được cấp. Hệ thống xác thực và khởi tạo phiên.
- **UC-1.2 Đăng xuất**: Cho phép người dùng kết thúc phiên làm việc hiện tại, thu hồi trạng thái truy cập tạm thời.
- **UC-1.3 Khôi phục mật khẩu**: Yêu cầu khôi phục thông qua Email xác thực.
- **UC-1.4 Quản lý hồ sơ cá nhân**: Xem và cập nhật thông tin cá nhân được phép chỉnh sửa.
- **UC-1.5 Tạo tài khoản người dùng**: Workspace Owner/Admin tạo tài khoản, thiết lập role ban đầu.
- **UC-1.6 Xem danh sách tài khoản người dùng**: Xem danh sách tài khoản trong hệ thống.
- **UC-1.7 Cập nhật thông tin tài khoản người dùng**: Cập nhật thông tin quản trị của người dùng.
- **UC-1.8 Khoá/Mở khoá tài khoản**: Quản trị viên thay đổi trạng thái truy cập của người dùng.
- **UC-1.9 Cập nhật vai trò người dùng**: Thay đổi vai trò và điều chỉnh quyền truy cập tương ứng.

## UC-2.0: Quản lý Workspace / Tổ chức
- **UC-2.1 Tạo và thiết lập thông tin ban đầu cho Workspace**: Khởi tạo không gian làm việc cho tổ chức/doanh nghiệp.
- **UC-2.2 Cập nhật thông tin Workspace**: Cập nhật thông tin cơ bản của Workspace.
- **UC-2.3 Tạo nhóm làm việc trong Workspace**: Chia nhỏ cơ cấu tổ chức thành các phòng ban.
- **UC-2.4 Cập nhật thông tin nhóm làm việc trong Workspace**: Thay đổi thông tin của nhóm.
- **UC-2.5 Giải thể nhóm**: Chuyển nhóm sang trạng thái không hoạt động.
- **UC-2.6 Cập nhật thông tin tổ chức của thành viên**: Điều chuyển nhân sự giữa các Team.
- **UC-2.7 Gán Team Leader cho nhóm**: Chỉ định người quản lý Team.
- **UC-2.8 Phân bổ thành viên vào dự án và gán vai trò**: Team Leader chỉ định Project Manager và Team Members cho dự án.
- **UC-2.9 Theo dõi thông tin hoạt động của tổ chức**: Cung cấp cái nhìn tổng quan về cơ cấu và nhân sự.
- **UC-2.10 Đóng / Vô hiệu hóa Workspace**: Tạm ngừng toàn bộ hoạt động trong Workspace.

## UC-3.0: Quản lý Dự án
- **UC-3.1 Tạo dự án mới**: Team Leader tạo dự án (sinh mã dự án tự động, trạng thái Draft).
- **UC-3.2 Xem chi tiết dự án**: Xem thông tin chi tiết và điều hướng các tab chức năng.
- **UC-3.3 Cập nhật thông tin dự án**: PM/Team Leader thay đổi thông tin dự án.
- **UC-3.4 Quản lý thành viên dự án**: Thêm/xóa và gán quyền thao tác trong phạm vi dự án.
- **UC-3.5 Kích hoạt dự án**: Chuyển từ Draft sang Active.
- **UC-3.6 Xem danh sách thành viên dự án**: Xem danh sách các thành viên trong dự án.
- **UC-3.7 Xem danh sách dự án**: Hiển thị danh sách dự án theo quyền hạn.
- **UC-3.8 Quản lý tài liệu dự án**: Upload, phân loại tài liệu vào folder.
- **UC-3.9 Xem danh sách tài liệu dự án**: Tải xuống và xem các tệp tài liệu.
- **UC-3.10 Xem lịch sử hoạt động dự án**: Theo dõi các sự kiện thay đổi trong dự án.
- **UC-3.11 Kết thúc dự án**: Project Manager chuyển trạng thái sang Completed khi hoàn thành mục tiêu.
- **UC-3.12 Lưu trữ dự án**: Chuyển sang Archived để bảo toàn dữ liệu lịch sử.

## UC-4.0: Quản lý Product Backlog và Công việc
- **UC-4.1 Tạo Product Backlog Item**: Tạo PBI ghi nhận yêu cầu, tính năng.
- **UC-4.2 Cập nhật Product Backlog Item**: Chỉnh sửa thông tin PBI.
- **UC-4.3 Xóa Product Backlog Item**: Xóa PBI không cần thiết.
- **UC-4.4 Tra cứu Product Backlog Item**: Xem danh sách các PBI.
- **UC-4.5 Phân rã PBI thành Task**: Tách PBI thành các task chuẩn bị (Draft Task) để lập kế hoạch.
- **UC-4.6 Cập nhật Task chuẩn bị**: Chỉnh sửa thông tin task đã phân rã.
- **UC-4.7 Xóa Task chuẩn bị**: Xóa task chuẩn bị.
- **UC-4.8 Gán thành viên dự kiến cho Task**: Phân công dự kiến nguồn lực.
- **UC-4.9 Xác nhận PBI sẵn sàng**: Đổi trạng thái PBI sang Ready để chờ đưa vào Sprint.
- **UC-4.10 Tìm kiếm và lọc Product Backlog Item**: Lọc PBI theo các tiêu chí.

## UC-5.0: Quản lý Sprint / Workflow và quy trình vận hành
- **UC-5.1 Quản lý Sprint**: Tạo Sprint, xác định mục tiêu và thời gian triển khai.
- **UC-5.2 Quản lý PBI trong Sprint**: Chuyển các PBI (Ready) vào Sprint.
- **UC-5.3 Quản lý công việc trong Sprint**: Kích hoạt task để đưa vào Sprint.
- **UC-5.4 Rà soát và xác nhận phân công công việc**: Chốt người phụ trách cho task.
- **UC-5.5 Thực hiện công việc trong Sprint**: Team Member cập nhật trạng thái Task (To Do -> In Progress -> Review -> Done).
- **UC-5.6 Theo dõi tiến độ Sprint**: Theo dõi tổng quan tiến độ của Sprint.
- **UC-5.7 Kiểm tra và phê duyệt công việc**: PM đánh giá kết quả, chuyển sang Done hoặc Reopened.
- **UC-5.8 Tổng kết Sprint**: Đánh giá mức độ hoàn thành và chuyển trạng thái Sprint (Completed/Canceled).
- **UC-5.9 Tra cứu lịch sử Sprint**: Xem các thay đổi trong quá trình chạy Sprint.

## UC-6.0: Quản lý Cộng tác và trao đổi
- **UC-6.1 Tạo bình luận trao đổi**: Thảo luận trực tiếp trên từng Task.
- **UC-6.2 Chỉnh sửa hoặc xóa bình luận**: Quản lý nội dung đã gửi.
- **UC-6.3 Đính kèm tệp vào trao đổi**: Hỗ trợ trao đổi tài liệu, hình ảnh minh họa cho công việc.
- **UC-6.4 Xem trao đổi trong task**: Xem lịch sử các phản hồi, bình luận.

## UC-7.0: Quản lý Dashboard
- **UC-7.1 Xem Dashboard tổng quan Workspace**: Số liệu tổng quan toàn bộ không gian làm việc.
- **UC-7.2 Xem Dashboard Team**: Tiến độ và workload trong phạm vi nhóm.
- **UC-7.3 Xem Dashboard Dự án**: Theo dõi tiến độ, số lượng task, sprint của dự án (dành cho PM).
- **UC-7.4 Xem Dashboard Cá nhân**: Tập trung vào task được giao, sắp đến hạn của riêng thành viên.