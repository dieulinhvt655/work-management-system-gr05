package com.workmanagement.backend.user.controller;

import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.response.PageResponse;
import com.workmanagement.backend.user.dto.request.CreateUserRequest;
import com.workmanagement.backend.user.dto.request.UpdateProfileRequest;
import com.workmanagement.backend.user.dto.request.UpdateUserRoleRequest;
import com.workmanagement.backend.user.dto.response.UserResponse;
import com.workmanagement.backend.user.dto.response.UserRoleResponse;
import com.workmanagement.backend.user.service.UserService;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCurrentProfile_shouldReturn200() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("admin@test.com")
                .fullName("Admin")
                .build();

        when(userService.getCurrentProfile()).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("admin@test.com"));
    }

    @Test
    void updateProfile_shouldReturn200() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .fullName("Updated")
                .description("Updated description")
                .build();

        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Updated","phone":"0901234567"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated"));
    }

    @Test
    void create_shouldReturn201Payload() throws Exception {
        UserResponse response = UserResponse.builder().id(2L).email("new@test.com").build();

        when(userService.create(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"New",
                                  "email":"new@test.com",
                                  "username":"newuser",
                                  "workspaceId":10,
                                  "teamId":5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("new@test.com"));
    }

    @Test
    void create_shouldReturn201PayloadWithoutDepartment() throws Exception {
        UserResponse response = UserResponse.builder().id(2L).email("new@test.com").build();

        when(userService.create(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"New",
                                  "email":"new@test.com",
                                  "username":"newuser",
                                  "workspaceId":10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("new@test.com"));
    }

    @Test
    void findAll_shouldReturnPagedList() throws Exception {
        PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                .items(List.of(UserResponse.builder().id(1L).email("admin@test.com").build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(userService.findAll(0, 20, null, null, null, null, null, "createdAt", "desc"))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].email").value("admin@test.com"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].refreshToken").doesNotExist());

        verify(userService).findAll(
                0, 20, null, null, null, null, null, "createdAt", "desc");
    }

    @Test
    void findAll_shouldForwardFiltersAndSort() throws Exception {
        PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                .items(List.of())
                .page(1)
                .size(50)
                .totalElements(0)
                .totalPages(0)
                .build();
        when(userService.findAll(
                1, 50, "NV001", UserStatus.ACTIVE, 10L, 2L, 5L, "employeeCode", "asc"))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "1")
                        .param("size", "50")
                        .param("keyword", "NV001")
                        .param("status", "ACTIVE")
                        .param("workspaceId", "10")
                        .param("roleId", "2")
                        .param("teamId", "5")
                        .param("sortBy", "employeeCode")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk());

        verify(userService).findAll(
                1, 50, "NV001", UserStatus.ACTIVE, 10L, 2L, 5L, "employeeCode", "asc");
    }

    @Test
    void findById_shouldReturnUser() throws Exception {
        UserResponse response = UserResponse.builder().id(1L).email("admin@test.com").build();

        when(userService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void updateStatus_shouldReturnUpdatedUser() throws Exception {
        UserResponse response = UserResponse.builder().id(2L).status(UserStatus.INACTIVE).build();

        when(userService.updateStatus(eq(2L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void updateStatus_shouldRejectMissingOrUnknownStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"LOCKED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturnUpdatedUser() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .fullName("Updated")
                .description("Updated description")
                .build();

        when(userService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Updated","description":"Updated description"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated"))
                .andExpect(jsonPath("$.data.description").value("Updated description"));
    }

    @Test
    void updateUserRole_shouldReturnUpdatedRole() throws Exception {
        UserRoleResponse response = UserRoleResponse.builder()
                .userId(2L)
                .workspaceId(10L)
                .role(com.workmanagement.backend.security.dto.response.RoleResponse.builder()
                        .id(1L)
                        .name("Workspace Member")
                        .scope(RoleScope.WORKSPACE)
                        .build())
                .build();

        when(userService.updateUserRole(eq(2L), any(UpdateUserRoleRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roleId":1,"workspaceId":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workspaceId").value(10))
                .andExpect(jsonPath("$.data.role.name").value("Workspace Member"));
    }


}
