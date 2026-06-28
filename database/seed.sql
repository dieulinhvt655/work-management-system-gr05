USE work_management_system;

-- =============================================================================
-- Seed: roles
-- 7 vai trò mặc định theo actors trong tài liệu yêu cầu (glossary.md, test.md)
-- id do AUTO_INCREMENT sinh tự động — không gán thủ công
-- Chạy sau schema.sql: mysql -u <user> -p < database/seed.sql
-- =============================================================================

INSERT INTO roles (name, description, scope)
SELECT s.name, s.description, s.scope
FROM (
    SELECT
        'System Admin' AS name,
        'Quản trị viên hệ thống, chịu trách nhiệm cấu hình hệ thống, quản lý người dùng và hỗ trợ vận hành hệ thống.' AS description,
        'system' AS scope
    UNION ALL SELECT
        'Workspace Owner',
        'Người sở hữu và quản trị cao nhất của Workspace, chịu trách nhiệm quản lý tổ chức và toàn bộ hoạt động trong Workspace.',
        'workspace'
    UNION ALL SELECT
        'Workspace Member',
        'Thành viên tham gia Workspace, thực hiện công việc theo phân quyền được giao trong phạm vi tổ chức.',
        'workspace'
    UNION ALL SELECT
        'Team Leader',
        'Người quản lý các nhóm làm việc trong tổ chức, chịu trách nhiệm quản lý nhân sự và nguồn lực thuộc phạm vi được giao.',
        'team'
    UNION ALL SELECT
        'Team Member',
        'Thành viên tham gia nhóm làm việc, phối hợp thực hiện công việc chung trong phạm vi team.',
        'team'
    UNION ALL SELECT
        'Project Manager',
        'Người quản lý dự án, chịu trách nhiệm lập kế hoạch, điều phối nguồn lực và theo dõi tiến độ thực hiện dự án.',
        'project'
    UNION ALL SELECT
        'Project Contributor',
        'Thành viên thực hiện công việc, tham gia dự án và xử lý các nhiệm vụ được giao trong phạm vi project.',
        'project'
) AS s
LEFT JOIN roles r ON r.name = s.name AND r.scope = s.scope
WHERE r.id IS NULL;

SELECT * FROM roles;

SELECT * FROM permissions;


