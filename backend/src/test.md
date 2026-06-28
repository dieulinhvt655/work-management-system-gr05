API test Role & Permission

Kết luận ngắn

Có API để test role với permission. Không thiếu endpoint — có thể bạn chưa thấy vì chưa có Swagger/Postman collection trong repo.

Các controller đã implement:







Module



Base path



File





Role



/api/v1/roles



RoleController.java





Permission



/api/v1/permissions



PermissionController.java





Gán role cho user



PATCH /api/v1/users/{id}/role



UserController.java

flowchart LR
login["POST /auth/login"] --> token[JWT Bearer]
token --> listPerm["GET /permissions"]
token --> listRole["GET /roles"]
token --> assign["PUT /roles/{id}/permissions"]
listPerm --> assign
assign --> getRole["GET /roles/{id}"]
getRole --> roleWithPerms["RoleResponse.permissions"]

Endpoints liên quan Role ↔ Permission

Permission (/api/v1/permissions)





GET /api/v1/permissions — danh sách tất cả (filter: ?module=user)



GET /api/v1/permissions/{id} — chi tiết 1 quyền



POST /api/v1/permissions — tạo quyền mới



PUT /api/v1/permissions/{id} — cập nhật



DELETE /api/v1/permissions/{id} — xóa

Role (/api/v1/roles)





GET /api/v1/roles — danh sách role kèm permissions (filter: ?scope=SYSTEM|WORKSPACE|TEAM|PROJECT)



GET /api/v1/roles/{id} — chi tiết role kèm permissions



POST /api/v1/roles — tạo role (tùy chọn permissionIds ngay khi tạo)



PUT /api/v1/roles/{id} — cập nhật (nếu gửi permissionIds sẽ thay thế toàn bộ)



PUT /api/v1/roles/{id}/permissions — UC-1.9: gán/thay thế danh sách permission cho role



DELETE /api/v1/roles/{id} — xóa role (xóa luôn mapping trong role_permissions)

RoleResponse đã trả về permissions: List<PermissionResponse> — không cần endpoint riêng GET /roles/{id}/permissions.

User ↔ Role (system level)





PATCH /api/v1/users/{id}/role — body { "roleId": 1 } gán system role cho user (UC-1.9)

Luồng test bằng curl

Điều kiện: App chạy tại http://localhost:8080, MySQL đã seed qua DataInitializer.java.

Bước 1 — Login lấy token

curl -s -X POST http://localhost:8080/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{"email":"admin@workmanagement.local","password":"admin123"}'

Lấy data.accessToken từ response, gán biến:

export TOKEN="<accessToken>"

Bước 2 — Xem permission đã seed

curl -s http://localhost:8080/api/v1/permissions \
-H "Authorization: Bearer $TOKEN"

Filter theo module:

curl -s "http://localhost:8080/api/v1/permissions?module=workspace" \
-H "Authorization: Bearer $TOKEN"

Bước 3 — Xem role kèm permission

Tất cả role:

curl -s http://localhost:8080/api/v1/roles \
-H "Authorization: Bearer $TOKEN"

Theo scope (ví dụ Team Leader):

curl -s "http://localhost:8080/api/v1/roles?scope=TEAM" \
-H "Authorization: Bearer $TOKEN"

Chi tiết 1 role (thay {id}):

curl -s http://localhost:8080/api/v1/roles/1 \
-H "Authorization: Bearer $TOKEN"

Response mẫu sẽ có dạng:

{
"data": {
"id": 1,
"name": "System Admin",
"scope": "SYSTEM",
"permissions": [
{ "id": 1, "code": "user:create", "module": "user", ... }
]
}
}

Bước 4 — Gán permission cho role (UC-1.9)

Thay {roleId} và danh sách permissionIds từ bước 2:

curl -s -X PUT http://localhost:8080/api/v1/roles/{roleId}/permissions \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"permissionIds":[1,2,3,4]}'

Lưu ý: Endpoint này thay thế toàn bộ permission cũ (xóa hết rồi gán lại) — logic trong RoleService.assignPermissions.

Bước 5 — Tạo role mới kèm permission (tùy chọn)

curl -s -X POST http://localhost:8080/api/v1/roles \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{
"name": "Custom Role",
"description": "Role test",
"scope": "WORKSPACE",
"permissionIds": [14, 15, 16]
}'

Bước 6 — Gán system role cho user

curl -s -X PATCH http://localhost:8080/api/v1/users/1/role \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"roleId": 1}'

Dữ liệu seed sẵn (sau khi chạy app)

DataInitializer seed 7 role theo Actors trong tài liệu:







Role



Scope





System Admin



SYSTEM





Workspace Owner



WORKSPACE





Workspace Member



WORKSPACE





Team Leader



TEAM





Team Member



TEAM





Project Manager



PROJECT





Team Member



PROJECT

Cùng ~50 permission (user, role, workspace, team, project, backlog, sprint, task, comment, attachment, notification, dashboard) và mapping RBAC tương ứng.

Hạn chế hiện tại (không phải thiếu API)





Chưa có Swagger/OpenAPI — không có UI explore API trong browser; phải dùng curl/Postman thủ công.



Chưa enforce RBAC trên endpoint — SecurityConfig chỉ yêu cầu authenticated(), chưa @PreAuthorize("hasAuthority('role:read')"). Mọi user đã login đều gọi được API role/permission.



JWT chưa chứa permissions — token chỉ xác thực identity; permission chỉ dùng khi gán role, chưa check khi gọi API khác.

Việc có thể làm thêm (nếu bạn muốn)





Thêm springdoc-openapi để có Swagger UI tại /swagger-ui.html



Tạo file documents/api-test/role-permission.http hoặc Postman collection trong repo



Bật @PreAuthorize theo permission code để test RBAC thật sự

Không cần viết thêm API mới chỉ để "xem role có permission gì" — GET /api/v1/roles và GET /api/v1/roles/{id} đã đủ.