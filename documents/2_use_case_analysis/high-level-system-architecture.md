# High level system architecture

## Mô tả kiến trúc được lựa chọn
Hệ thống Quản lý Công việc được xây dựng theo mô hình Client–Server Architecture kết hợp với Layered Architecture ở phía Server. Đây là kiến trúc phù hợp với hệ thống, vì người dùng truy cập và sử dụng hệ thống thông qua trình duyệt web, trong khi các hoạt động xử lý nghiệp vụ, quản lý dữ liệu và kiểm soát quyền truy cập được thực hiện tập trung tại phía Server.

Ở mức tổng quát, người dùng sử dụng Client để tương tác với hệ thống. Các yêu cầu từ người dùng được gửi đến Web Server, sau đó được chuyển tiếp đến Application Server để xử lý. Application Server đảm nhiệm việc thực hiện các nghiệp vụ chính của hệ thống, đồng thời làm việc với Database Server khi cần truy xuất hoặc cập nhật dữ liệu. Sau khi xử lý xong, kết quả sẽ được trả về Client thông qua Web Server.

Bên trong Application Server, hệ thống được tổ chức theo kiến trúc phân lớp nhằm tách biệt rõ ràng các trách nhiệm xử lý. Mỗi tầng trong hệ thống đảm nhận một vai trò riêng, chẳng hạn như tiếp nhận yêu cầu, xử lý nghiệp vụ và truy xuất dữ liệu. Cách tổ chức này giúp hệ thống có cấu trúc rõ ràng, dễ bảo trì, dễ kiểm thử và thuận tiện hơn khi cần mở rộng thêm chức năng trong tương lai.

Việc lựa chọn Client–Server Architecture là phù hợp vì hệ thống Quản lý Công việc cần quản lý dữ liệu tập trung, hỗ trợ nhiều người dùng cùng truy cập và đảm bảo kiểm soát quyền hạn trong quá trình sử dụng. Bên cạnh đó, Layered Architecture giúp tách biệt phần giao diện, phần xử lý nghiệp vụ và phần truy xuất dữ liệu. Nhờ vậy, khi có thay đổi ở một thành phần, các thành phần khác sẽ ít bị ảnh hưởng hơn.

## Mô tả các thành phần kiến trúc
*   **Client:** Là thiết bị đầu cuối mà người dùng sử dụng để tương tác với hệ thống Quản lý Công việc. Trong phạm vi hệ thống này, Client chủ yếu là máy tính hoặc thiết bị có kết nối Internet, có thể truy cập hệ thống thông qua trình duyệt web. Client đóng vai trò hiển thị giao diện, tiếp nhận thao tác của người dùng và gửi các yêu cầu nghiệp vụ đến hệ thống.
*   **Web Server:** Đóng vai trò là lớp trung gian giữa Client và Application Server. Web Server chịu trách nhiệm tiếp nhận các yêu cầu HTTP/HTTPS từ Client, xử lý định tuyến request và chuyển các yêu cầu cần xử lý nghiệp vụ đến Application Server.
*   **Application Server:** Là thành phần chịu trách nhiệm chính trong việc xử lý nghiệp vụ của hệ thống. Bên trong Application Server, hệ thống được tổ chức theo các tầng:
    *   **Presentation Layer:** Tầng tiếp nhận yêu cầu từ phía người dùng thông qua Web Server, xử lý request, điều hướng luồng xử lý và trả kết quả về giao diện.
    *   **Business Layer:** Tầng xử lý các nghiệp vụ chính của hệ thống. Tầng này chịu trách nhiệm thực hiện các quy tắc nghiệp vụ, kiểm tra điều kiện xử lý, kiểm soát quyền.
    *   **Data Access Layer:** Tầng trung gian giữa Business Layer và Database Server. Tầng này chịu trách nhiệm thực hiện các thao tác truy xuất, thêm mới, cập nhật hoặc xóa dữ liệu.
*   **Database Server:** Là nơi lưu trữ dữ liệu chính của toàn bộ hệ thống. Quản lý các dữ liệu phục vụ hoạt động của hệ thống như thông tin người dùng, dự án, công việc, thông báo, nhật ký hoạt động.