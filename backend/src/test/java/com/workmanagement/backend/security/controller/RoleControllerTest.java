package com.workmanagement.backend.security.controller;

import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.security.dto.response.RoleResponse;
import com.workmanagement.backend.security.service.RoleService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_shouldReturnRoles() throws Exception {
        RoleResponse role = RoleResponse.builder().id(1L).name("System Admin").scope(RoleScope.SYSTEM).build();
        when(roleService.findAll(null)).thenReturn(List.of(role));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("System Admin"));
    }

    @Test
    void findById_shouldReturnRole() throws Exception {
        RoleResponse role = RoleResponse.builder().id(1L).name("System Admin").build();
        when(roleService.findById(1L)).thenReturn(role);

        mockMvc.perform(get("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_shouldReturnCreatedRole() throws Exception {
        RoleResponse role = RoleResponse.builder().id(2L).name("Custom Role").scope(RoleScope.WORKSPACE).build();
        when(roleService.create(any())).thenReturn(role);

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Custom Role","description":"Test","scope":"WORKSPACE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Custom Role"));
    }

    @Test
    void assignPermissions_shouldReturnUpdatedRole() throws Exception {
        RoleResponse role = RoleResponse.builder().id(1L).name("System Admin").build();
        when(roleService.assignPermissions(eq(1L), any())).thenReturn(role);

        mockMvc.perform(put("/api/v1/roles/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionIds":[1,2]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Gán quyền thành công"));
    }

    @Test
    void delete_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(roleService).delete(1L);
    }

}
