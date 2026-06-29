package com.workmanagement.backend.integration.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workmanagement.backend.auth.dto.request.LoginRequest;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.productbacklog.dto.request.CreateProductBacklogItemRequest;
import com.workmanagement.backend.project.dto.request.CreateProjectRequest;
import com.workmanagement.backend.task.dto.request.CreateTaskRequest;
import com.workmanagement.backend.team.dto.request.AddTeamMemberRequest;
import com.workmanagement.backend.team.dto.request.CreateTeamRequest;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import com.workmanagement.backend.user.util.EmployeeCodeGenerator;
import com.workmanagement.backend.workspace.dto.request.CreateWorkspaceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@workmanagement.local";
    protected static final String ADMIN_PASSWORD = "admin123";

    private static final AtomicLong UNIQUE_COUNTER = new AtomicLong();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    protected EmployeeCodeGenerator employeeCodeGenerator;

    protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected String uniqueId() {
        return Long.toHexString(UNIQUE_COUNTER.incrementAndGet());
    }

    protected RequestPostProcessor bearer(String accessToken) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + accessToken);
            return request;
        };
    }

    protected JsonNode readRoot(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode readData(MvcResult result) throws Exception {
        return readRoot(result).get("data");
    }

    protected LoginTokens login(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode data = readData(result);
        return new LoginTokens(data.get("accessToken").asText(), data.get("refreshToken").asText());
    }

    protected LoginTokens loginAsAdmin() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected LoginTokens registerAndLogin(String suffix) throws Exception {
        String email = "integration-" + suffix + "@test.local";
        String username = "integration_" + suffix;
        String password = "password123";

        userRepository.save(User.builder()
                .fullName("Integration User " + suffix)
                .email(email)
                .username(username)
                .employeeCode(employeeCodeGenerator.generateUnique())
                .passwordHash(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .build());

        return login(email, password);
    }

    protected Long findRoleId(String accessToken, String roleName, RoleScope scope) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/roles")
                        .with(bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode roles = readData(result);
        for (JsonNode role : roles) {
            if (roleName.equals(role.get("name").asText())
                    && scope.name().equals(role.get("scope").asText())) {
                return role.get("id").asLong();
            }
        }
        throw new IllegalStateException("Role not found: " + roleName);
    }

    protected ProjectTestContext setupPmProject(String suffix) throws Exception {
        LoginTokens tokens = loginAsAdmin();

        CreateWorkspaceRequest workspaceRequest = new CreateWorkspaceRequest();
        workspaceRequest.setName("Workspace " + suffix);
        workspaceRequest.setDescription("Integration test workspace");

        MvcResult workspaceResult = mockMvc.perform(post("/api/v1/workspaces")
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workspaceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        Long workspaceId = readData(workspaceResult).get("id").asLong();

        CreateTeamRequest teamRequest = new CreateTeamRequest();
        teamRequest.setName("Team " + suffix);

        MvcResult teamResult = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/teams", workspaceId)
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teamRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        Long teamId = readData(teamResult).get("id").asLong();

        Long workspaceMemberId = findAdminWorkspaceMemberId(tokens.accessToken(), workspaceId);
        Long teamLeaderRoleId = findRoleId(tokens.accessToken(), "Team Leader", RoleScope.TEAM);

        AddTeamMemberRequest addMemberRequest = new AddTeamMemberRequest();
        addMemberRequest.setWorkspaceMemberId(workspaceMemberId);
        addMemberRequest.setRoleId(teamLeaderRoleId);

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/teams/{teamId}/members",
                        workspaceId, teamId)
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMemberRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Long teamMemberId = findTeamMemberId(tokens.accessToken(), workspaceId, teamId);

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setCode("PRJ-" + suffix);
        projectRequest.setName("Project " + suffix);
        projectRequest.setDescription("Integration test project");
        projectRequest.setStartDate(LocalDate.now());
        projectRequest.setEndDate(LocalDate.now().plusMonths(3));
        projectRequest.setProjectManagerMemberId(teamMemberId);

        MvcResult projectResult = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects",
                        workspaceId, teamId)
                        .with(bearer(tokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        Long projectId = readData(projectResult).get("id").asLong();

        mockMvc.perform(patch(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/activate",
                        workspaceId, teamId, projectId)
                        .with(bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        return new ProjectTestContext(tokens, workspaceId, teamId, projectId, teamMemberId);
    }

    protected Long createPbi(ProjectTestContext context, String title) throws Exception {
        CreateProductBacklogItemRequest request = new CreateProductBacklogItemRequest();
        request.setTitle(title);

        MvcResult result = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/backlog/items",
                        context.workspaceId(), context.teamId(), context.projectId())
                        .with(bearer(context.tokens().accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return readData(result).get("id").asLong();
    }

    protected Long createPreparationTask(ProjectTestContext context, Long pbiId, String title) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(title);

        MvcResult result = mockMvc.perform(post(
                        "/api/v1/workspaces/{workspaceId}/teams/{teamId}/projects/{projectId}/backlog/items/{itemId}/tasks",
                        context.workspaceId(), context.teamId(), context.projectId(), pbiId)
                        .with(bearer(context.tokens().accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return readData(result).get("id").asLong();
    }

    private Long findAdminWorkspaceMemberId(String accessToken, Long workspaceId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .with(bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        for (JsonNode member : readData(result)) {
            if (ADMIN_EMAIL.equals(member.path("user").path("email").asText())) {
                return member.get("id").asLong();
            }
        }
        throw new IllegalStateException("Admin workspace member not found");
    }

    private Long findTeamMemberId(String accessToken, Long workspaceId, Long teamId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/teams/{teamId}/members",
                        workspaceId, teamId)
                        .with(bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        for (JsonNode member : readData(result)) {
            if (ADMIN_EMAIL.equals(member.path("user").path("email").asText())) {
                return member.get("id").asLong();
            }
        }
        throw new IllegalStateException("Admin team member not found");
    }
}
