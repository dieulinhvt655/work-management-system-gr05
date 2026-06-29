USE work_management_system;

# CREATE TABLE users (
#                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
#                                      full_name VARCHAR(255) NOT NULL,
#                                      email VARCHAR(255) NOT NULL UNIQUE,
#                                      username VARCHAR(100) NOT NULL UNIQUE,
#                                      password_hash VARCHAR(255) NOT NULL,
#                                      phone VARCHAR(20),
#                                      avatar_url VARCHAR(500),
#                                      employee_code VARCHAR(6) NOT NULL UNIQUE,
#                                      description TEXT,
#                                      status ENUM('active', 'inactive', 'deleted') NOT NULL DEFAULT 'active',
#                                      role_id BIGINT,
#                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
#                                      updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
#
#                                      CONSTRAINT fk_users_role
#                                          FOREIGN KEY (role_id) REFERENCES roles(id)
#                                              ON UPDATE CASCADE ON DELETE SET NULL
# );

# ===============================
# MIGRATION — users: employee_code (6 chữ số), description
# Ghi chú:
#   - departmentId khi tạo user là field API (map team_id), KHÔNG có cột trên users.
#   - Gán phòng ban/team qua bảng team_members (workspace_member_id + team_id).
# Chạy từng bước nếu DB đã tồn tại trước khi có các cột mới.
# Bỏ qua bước nào đã được Hibernate ddl-auto=update áp dụng.
# ===============================

# -- Bước 1: thêm cột (nullable tạm thời cho employee_code)
# ALTER TABLE users
#     ADD COLUMN employee_code VARCHAR(6) NULL AFTER avatar_url,
#     ADD COLUMN description TEXT NULL AFTER employee_code;

# -- Bước 2: backfill mã nhân viên 6 chữ số cho bản ghi cũ (chỉ số, duy nhất theo id)
# UPDATE users u
#     JOIN (
#         SELECT id, LPAD(id, 6, '0') AS generated_code
#         FROM users
#         WHERE employee_code IS NULL
#     ) AS src ON u.id = src.id
# SET u.employee_code = src.generated_code;

# -- Bước 3: ràng buộc NOT NULL + UNIQUE
# ALTER TABLE users
#     MODIFY COLUMN employee_code VARCHAR(6) NOT NULL,
#     ADD CONSTRAINT uk_users_employee_code UNIQUE (employee_code);

# -- (Tùy chọn) Chỉ thêm description nếu chưa có employee_code migration
# ALTER TABLE users
#     ADD COLUMN description TEXT NULL AFTER avatar_url;

# -- (Tùy chọn) Thêm role_id nếu bảng users cũ chưa có
# ALTER TABLE users
#     ADD COLUMN role_id BIGINT NULL AFTER status,
#     ADD CONSTRAINT fk_users_role
#         FOREIGN KEY (role_id) REFERENCES roles(id)
#             ON UPDATE CASCADE ON DELETE SET NULL;

CREATE TABLE roles (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(100) NOT NULL,
                                     description TEXT,
                                     scope VARCHAR(50) NOT NULL,
                                     CHECK (scope IN ('workspace', 'team', 'project', 'system'))
);

CREATE TABLE permissions (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           code VARCHAR(100) NOT NULL UNIQUE,
                                           name VARCHAR(100) NOT NULL,
                                           module VARCHAR(100) NOT NULL,
                                           description TEXT
);

CREATE TABLE role_permissions (
                                                role_id BIGINT NOT NULL,
                                                permission_id BIGINT NOT NULL,
                                                PRIMARY KEY (role_id, permission_id),

                                                CONSTRAINT fk_role_permissions_role
                                                    FOREIGN KEY (role_id) REFERENCES roles(id)
                                                        ON UPDATE CASCADE ON DELETE CASCADE,

                                                CONSTRAINT fk_role_permissions_permission
                                                    FOREIGN KEY (permission_id) REFERENCES permissions(id)
                                                        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                user_id BIGINT NOT NULL,
                                                token_hash VARCHAR(64) NOT NULL UNIQUE,
                                                expires_at DATETIME NOT NULL,
                                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                CONSTRAINT fk_refresh_tokens_user
                                                    FOREIGN KEY (user_id) REFERENCES users(id)
                                                        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE workspaces (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          owner_user_id BIGINT NOT NULL,
                                          name VARCHAR(255) NOT NULL,
                                          description TEXT,
                                          status ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
                                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                                          CONSTRAINT fk_workspaces_owner_user
                                              FOREIGN KEY (owner_user_id) REFERENCES users(id)
                                                  ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE workspace_members (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 workspace_id BIGINT NOT NULL,
                                                 user_id BIGINT NOT NULL,
                                                 role_id BIGINT NOT NULL,
                                                 added_by_owner_id BIGINT,
                                                 status ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
                                                 joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                 removed_at DATETIME,

                                                 UNIQUE KEY uq_workspace_members_workspace_user (workspace_id, user_id),

                                                 CONSTRAINT fk_workspace_members_workspace
                                                     FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
                                                         ON UPDATE CASCADE ON DELETE CASCADE,

                                                 CONSTRAINT fk_workspace_members_user
                                                     FOREIGN KEY (user_id) REFERENCES users(id)
                                                         ON UPDATE CASCADE ON DELETE RESTRICT,

                                                 CONSTRAINT fk_workspace_members_role
                                                     FOREIGN KEY (role_id) REFERENCES roles(id)
                                                         ON UPDATE CASCADE ON DELETE RESTRICT,

                                                 CONSTRAINT fk_workspace_members_added_by_owner
                                                     FOREIGN KEY (added_by_owner_id) REFERENCES users(id)
                                                         ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE teams (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     workspace_id BIGINT NOT NULL,
                                     name VARCHAR(255) NOT NULL,
                                     description TEXT,
                                     status ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_teams_workspace
                                         FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
                                             ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE team_members (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            team_id BIGINT NOT NULL,
                                            workspace_member_id BIGINT NOT NULL,
                                            role_id BIGINT NOT NULL,
                                            added_by_workspace_member_id BIGINT,
                                            status ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
                                            joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            removed_at DATETIME,

                                            UNIQUE KEY uq_team_members_team_workspace_member (team_id, workspace_member_id),

                                            CONSTRAINT fk_team_members_team
                                                FOREIGN KEY (team_id) REFERENCES teams(id)
                                                    ON UPDATE CASCADE ON DELETE CASCADE,

                                            CONSTRAINT fk_team_members_workspace_member
                                                FOREIGN KEY (workspace_member_id) REFERENCES workspace_members(id)
                                                    ON UPDATE CASCADE ON DELETE RESTRICT,

                                            CONSTRAINT fk_team_members_role
                                                FOREIGN KEY (role_id) REFERENCES roles(id)
                                                    ON UPDATE CASCADE ON DELETE RESTRICT,

                                            CONSTRAINT fk_team_members_added_by_workspace_member
                                                FOREIGN KEY (added_by_workspace_member_id) REFERENCES workspace_members(id)
                                                    ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE projects (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        team_id BIGINT NOT NULL,
                                        project_manager_member_id BIGINT NOT NULL,
                                        code VARCHAR(50) NOT NULL UNIQUE,
                                        name VARCHAR(255) NOT NULL,
                                        description TEXT,
                                        objective TEXT,
                                        scope TEXT,
                                        start_date DATE,
                                        end_date DATE,
                                        status ENUM('draft', 'active', 'completed', 'archived') NOT NULL DEFAULT 'active',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                                        CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date),

                                        CONSTRAINT fk_projects_team
                                            FOREIGN KEY (team_id) REFERENCES teams(id)
                                                ON UPDATE CASCADE ON DELETE RESTRICT,

                                        CONSTRAINT fk_projects_project_manager_member
                                            FOREIGN KEY (project_manager_member_id) REFERENCES team_members(id)
                                                ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE project_members (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               project_id BIGINT NOT NULL,
                                               team_member_id BIGINT NOT NULL,
                                               role_id BIGINT NOT NULL,
                                               status ENUM('active', 'inactive') NOT NULL DEFAULT 'active',
                                               joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               removed_at DATETIME,

                                               UNIQUE KEY uq_project_members_project_team_member (project_id, team_member_id),

                                               CONSTRAINT fk_project_members_project
                                                   FOREIGN KEY (project_id) REFERENCES projects(id)
                                                       ON UPDATE CASCADE ON DELETE CASCADE,

                                               CONSTRAINT fk_project_members_team_member
                                                   FOREIGN KEY (team_member_id) REFERENCES team_members(id)
                                                       ON UPDATE CASCADE ON DELETE RESTRICT,

                                               CONSTRAINT fk_project_members_role
                                                   FOREIGN KEY (role_id) REFERENCES roles(id)
                                                       ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE product_backlogs (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                project_id BIGINT NOT NULL UNIQUE,
                                                name VARCHAR(255) NOT NULL,
                                                description TEXT,
                                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                                                CONSTRAINT fk_product_backlogs_project
                                                    FOREIGN KEY (project_id) REFERENCES projects(id)
                                                        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE sprints (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       project_id BIGINT NOT NULL,
                                       name VARCHAR(255) NOT NULL,
                                       goal TEXT,
                                       start_date DATE NOT NULL,
                                       end_date DATE NOT NULL,
                                       status ENUM('planning', 'active', 'completed', 'cancelled') NOT NULL DEFAULT 'planning',
                                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                                       CHECK (end_date >= start_date),

                                       CONSTRAINT fk_sprints_project
                                           FOREIGN KEY (project_id) REFERENCES projects(id)
                                               ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE product_backlog_items (
                                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     backlog_id BIGINT NOT NULL,
                                                     sprint_id BIGINT,
                                                     proposer_member_id BIGINT,
                                                     title VARCHAR(255) NOT NULL,
                                                     description TEXT,
                                                     type ENUM('feature', 'bug', 'improvement', 'task', 'other') NOT NULL DEFAULT 'feature',
                                                     priority ENUM('low', 'medium', 'high', 'urgent') NOT NULL DEFAULT 'medium',
                                                     status ENUM('new', 'ready', 'in_sprint', 'completed', 'on_hold') NOT NULL DEFAULT 'new',
                                                     desired_due_date DATE,
                                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                     updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                                                     CONSTRAINT fk_pbi_backlog
                                                         FOREIGN KEY (backlog_id) REFERENCES product_backlogs(id)
                                                             ON UPDATE CASCADE ON DELETE CASCADE,

                                                     CONSTRAINT fk_pbi_sprint
                                                         FOREIGN KEY (sprint_id) REFERENCES sprints(id)
                                                             ON UPDATE CASCADE ON DELETE SET NULL,

                                                     CONSTRAINT fk_pbi_proposer_member
                                                         FOREIGN KEY (proposer_member_id) REFERENCES project_members(id)
                                                             ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE workflow_states (
                                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               project_id BIGINT NOT NULL,
                                               name VARCHAR(100) NOT NULL,
                                               code VARCHAR(100) NOT NULL,
                                               position INT NOT NULL,
                                               is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                               is_final BOOLEAN NOT NULL DEFAULT FALSE,

                                               UNIQUE KEY uq_workflow_states_project_code (project_id, code),

                                               CONSTRAINT fk_workflow_states_project
                                                   FOREIGN KEY (project_id) REFERENCES projects(id)
                                                       ON UPDATE CASCADE ON DELETE CASCADE
);


CREATE TABLE workflow_transitions (
                                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    project_id BIGINT NOT NULL,
                                                    from_state_id BIGINT NOT NULL,
                                                    to_state_id BIGINT NOT NULL,
                                                    name VARCHAR(255),

                                                    UNIQUE KEY uq_workflow_transitions_project_from_to
                                                        (project_id, from_state_id, to_state_id),

                                                    CONSTRAINT fk_workflow_transitions_project
                                                        FOREIGN KEY (project_id) REFERENCES projects(id)
                                                            ON UPDATE CASCADE ON DELETE CASCADE,

                                                    CONSTRAINT fk_workflow_transitions_from_state
                                                        FOREIGN KEY (from_state_id) REFERENCES workflow_states(id)
                                                            ON UPDATE CASCADE ON DELETE CASCADE,

                                                    CONSTRAINT fk_workflow_transitions_to_state
                                                        FOREIGN KEY (to_state_id) REFERENCES workflow_states(id)
                                                            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE tasks (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     pbi_id BIGINT NOT NULL,
                                     sprint_id BIGINT,
                                     parent_task_id BIGINT,
                                     assignee_member_id BIGINT,
                                     reporter_member_id BIGINT NOT NULL,
                                     reviewer_member_id BIGINT,
                                     workflow_state_id BIGINT,
                                     title VARCHAR(255) NOT NULL,
                                     description TEXT,
                                     priority ENUM('low', 'medium', 'high', 'urgent') NOT NULL DEFAULT 'medium',
                                     status ENUM('to_do', 'in_progress', 'review', 'done', 'reopened', 'cancelled') NOT NULL DEFAULT 'to_do',
                                     progress INT NOT NULL DEFAULT 0,
                                     start_date DATE,
                                     deadline DATE,
                                     completed_at DATETIME,
                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                                     CHECK (progress BETWEEN 0 AND 100),
                                     CHECK (start_date IS NULL OR deadline IS NULL OR deadline >= start_date),

                                     CONSTRAINT fk_tasks_pbi
                                         FOREIGN KEY (pbi_id) REFERENCES product_backlog_items(id)
                                             ON UPDATE CASCADE ON DELETE CASCADE,

                                     CONSTRAINT fk_tasks_sprint
                                         FOREIGN KEY (sprint_id) REFERENCES sprints(id)
                                             ON UPDATE CASCADE ON DELETE SET NULL,

                                     CONSTRAINT fk_tasks_parent_task
                                         FOREIGN KEY (parent_task_id) REFERENCES tasks(id)
                                             ON UPDATE CASCADE ON DELETE SET NULL,

                                     CONSTRAINT fk_tasks_assignee_member
                                         FOREIGN KEY (assignee_member_id) REFERENCES project_members(id)
                                             ON UPDATE CASCADE ON DELETE SET NULL,

                                     CONSTRAINT fk_tasks_reporter_member
                                         FOREIGN KEY (reporter_member_id) REFERENCES project_members(id)
                                             ON UPDATE CASCADE ON DELETE RESTRICT,

                                     CONSTRAINT fk_tasks_reviewer_member
                                         FOREIGN KEY (reviewer_member_id) REFERENCES project_members(id)
                                             ON UPDATE CASCADE ON DELETE SET NULL,

                                     CONSTRAINT fk_tasks_workflow_state
                                         FOREIGN KEY (workflow_state_id) REFERENCES workflow_states(id)
                                             ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE comments (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        task_id BIGINT NOT NULL,
                                        project_member_id BIGINT NOT NULL,
                                        parent_comment_id BIGINT,
                                        content TEXT NOT NULL,
                                        status ENUM('active', 'edited', 'deleted') NOT NULL DEFAULT 'active',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                                        deleted_at DATETIME,

                                        CONSTRAINT fk_comments_task
                                            FOREIGN KEY (task_id) REFERENCES tasks(id)
                                                ON UPDATE CASCADE ON DELETE CASCADE,

                                        CONSTRAINT fk_comments_project_member
                                            FOREIGN KEY (project_member_id) REFERENCES project_members(id)
                                                ON UPDATE CASCADE ON DELETE RESTRICT,

                                        CONSTRAINT fk_comments_parent_comment
                                            FOREIGN KEY (parent_comment_id) REFERENCES comments(id)
                                                ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE attachments (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           uploaded_by_member_id BIGINT NOT NULL,
                                           project_id BIGINT,
                                           task_id BIGINT,
                                           comment_id BIGINT,
                                           file_name VARCHAR(255) NOT NULL,
                                           file_type VARCHAR(100),
                                           file_size BIGINT,
                                           file_url VARCHAR(500) NOT NULL,
                                           uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                           CHECK (file_size IS NULL OR file_size >= 0),

                                           CONSTRAINT fk_attachments_uploaded_by_member
                                               FOREIGN KEY (uploaded_by_member_id) REFERENCES project_members(id)
                                                   ON UPDATE CASCADE ON DELETE RESTRICT,

                                           CONSTRAINT fk_attachments_project
                                               FOREIGN KEY (project_id) REFERENCES projects(id)
                                                   ON UPDATE CASCADE ON DELETE CASCADE,

                                           CONSTRAINT fk_attachments_task
                                               FOREIGN KEY (task_id) REFERENCES tasks(id)
                                                   ON UPDATE CASCADE ON DELETE CASCADE,

                                           CONSTRAINT fk_attachments_comment
                                               FOREIGN KEY (comment_id) REFERENCES comments(id)
                                                   ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE notifications (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             receiver_user_id BIGINT NOT NULL,
                                             sender_user_id BIGINT,
                                             project_id BIGINT,
                                             pbi_id BIGINT,
                                             sprint_id BIGINT,
                                             task_id BIGINT,
                                             comment_id BIGINT,
                                             title VARCHAR(255) NOT NULL,
                                             content TEXT,
                                             type VARCHAR(100),
                                             status ENUM('unread', 'read') NOT NULL DEFAULT 'unread',
                                             sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             read_at DATETIME,

                                             CONSTRAINT fk_notifications_receiver_user
                                                 FOREIGN KEY (receiver_user_id) REFERENCES users(id)
                                                     ON UPDATE CASCADE ON DELETE CASCADE,

                                             CONSTRAINT fk_notifications_sender_user
                                                 FOREIGN KEY (sender_user_id) REFERENCES users(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL,

                                             CONSTRAINT fk_notifications_project
                                                 FOREIGN KEY (project_id) REFERENCES projects(id)
                                                     ON UPDATE CASCADE ON DELETE CASCADE,

                                             CONSTRAINT fk_notifications_pbi
                                                 FOREIGN KEY (pbi_id) REFERENCES product_backlog_items(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL,

                                             CONSTRAINT fk_notifications_sprint
                                                 FOREIGN KEY (sprint_id) REFERENCES sprints(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL,

                                             CONSTRAINT fk_notifications_task
                                                 FOREIGN KEY (task_id) REFERENCES tasks(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL,

                                             CONSTRAINT fk_notifications_comment
                                                 FOREIGN KEY (comment_id) REFERENCES comments(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE activity_logs (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             actor_user_id BIGINT NOT NULL,
                                             project_id BIGINT,
                                             pbi_id BIGINT,
                                             sprint_id BIGINT,
                                             task_id BIGINT,
                                             comment_id BIGINT,
                                             action VARCHAR(100) NOT NULL,
                                             target_type VARCHAR(100),
                                             target_id BIGINT,
                                             old_value TEXT,
                                             new_value TEXT,
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                             CONSTRAINT fk_activity_logs_actor_user
                                                 FOREIGN KEY (actor_user_id) REFERENCES users(id)
                                                     ON UPDATE CASCADE ON DELETE RESTRICT,

                                             CONSTRAINT fk_activity_logs_project
                                                 FOREIGN KEY (project_id) REFERENCES projects(id)
                                                     ON UPDATE CASCADE ON DELETE CASCADE,

                                             CONSTRAINT fk_activity_logs_pbi
                                                 FOREIGN KEY (pbi_id) REFERENCES product_backlog_items(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL,

                                             CONSTRAINT fk_activity_logs_sprint
                                                 FOREIGN KEY (sprint_id) REFERENCES sprints(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL,

                                             CONSTRAINT fk_activity_logs_task
                                                 FOREIGN KEY (task_id) REFERENCES tasks(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL,

                                             CONSTRAINT fk_activity_logs_comment
                                                 FOREIGN KEY (comment_id) REFERENCES comments(id)
                                                     ON UPDATE CASCADE ON DELETE SET NULL
);

SHOW TABLES;

SHOW TABLES;

SELECT * FROM users;

DELETE FROM users;

SELECT * FROM roles;

UPDATE roles SET name = 'Project Contributor' WHERE id = 7;

SELECT * FROM permissions;

SELECT * FROM users;

SELECT * FROM product_backlogs;

SELECT * FROM teams;

USE work_management_system;

SELECT * FROM users;

USE work_management_system;
