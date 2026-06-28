package com.workmanagement.backend.security.controller;

import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import com.workmanagement.backend.security.dto.response.PermissionResponse;
import com.workmanagement.backend.security.service.PermissionService;
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
class PermissionControllerTest {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(permissionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAll_shouldReturnPermissions() throws Exception {
        PermissionResponse permission = PermissionResponse.builder().id(1L).code("user:read").build();
        when(permissionService.findAll(null)).thenReturn(List.of(permission));

        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("user:read"));
    }

    @Test
    void findById_shouldReturnPermission() throws Exception {
        PermissionResponse permission = PermissionResponse.builder().id(1L).code("user:read").build();
        when(permissionService.findById(1L)).thenReturn(permission);

        mockMvc.perform(get("/api/v1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("user:read"));
    }

    @Test
    void create_shouldReturnCreatedPermission() throws Exception {
        PermissionResponse permission = PermissionResponse.builder().id(1L).code("user:create").build();
        when(permissionService.create(any())).thenReturn(permission);

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"user:create","name":"Create user","module":"user"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("user:create"));
    }

    @Test
    void update_shouldReturnUpdatedPermission() throws Exception {
        PermissionResponse permission = PermissionResponse.builder().id(1L).name("Updated").build();
        when(permissionService.update(eq(1L), any())).thenReturn(permission);

        mockMvc.perform(put("/api/v1/permissions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    void delete_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(permissionService).delete(1L);
    }

}
