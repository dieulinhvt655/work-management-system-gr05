package com.workmanagement.backend.attachment.service;

import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.attachment.config.AttachmentProperties;
import com.workmanagement.backend.attachment.dto.request.UpdateAttachmentRequest;
import com.workmanagement.backend.attachment.dto.response.AttachmentResponse;
import com.workmanagement.backend.attachment.entity.Attachment;
import com.workmanagement.backend.attachment.mapper.AttachmentMapper;
import com.workmanagement.backend.attachment.repository.AttachmentRepository;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.team.service.TeamService;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AttachmentMapper attachmentMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private TeamService teamService;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private AttachmentService attachmentService;

    @TempDir
    Path tempDir;

    private Project project;
    private ProjectMember projectMember;
    private Attachment attachment;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        Team team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        User user = User.builder().id(2L).fullName("PM").build();
        WorkspaceMember wsMember = WorkspaceMember.builder().id(5L).user(user).build();
        TeamMember teamMember = TeamMember.builder().id(7L).workspaceMember(wsMember).build();
        projectMember = ProjectMember.builder().id(11L).teamMember(teamMember).status(MemberStatus.ACTIVE).build();
        project = Project.builder()
                .id(30L)
                .team(team)
                .status(ProjectStatus.ACTIVE)
                .build();
        attachment = Attachment.builder()
                .id(40L)
                .project(project)
                .uploadedByMember(projectMember)
                .fileName("spec.pdf")
                .fileUrl("projects/30/uuid_spec.pdf")
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void findByProject_shouldReturnAttachments() {
        AttachmentResponse response = AttachmentResponse.builder().id(40L).fileName("spec.pdf").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(attachmentRepository.findByProjectIdOrderByUploadedAtDesc(30L)).thenReturn(List.of(attachment));
        when(attachmentMapper.toResponse(attachment)).thenReturn(response);

        List<AttachmentResponse> result = attachmentService.findByProject(10L, 20L, 30L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("spec.pdf");
    }

    @Test
    void upload_shouldStoreAttachment() {
        MultipartFile file = new MockMultipartFile("file", "spec.pdf", "application/pdf", "data".getBytes());
        FileStorageService.StoredFile storedFile = new FileStorageService.StoredFile(
                "projects/30/uuid_spec.pdf", "spec.pdf", "application/pdf", 4
        );
        AttachmentResponse response = AttachmentResponse.builder().id(40L).fileName("spec.pdf").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(project.getTeam());
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.of(projectMember));
        when(fileStorageService.storeProjectFile(30L, file)).thenReturn(storedFile);
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);
        when(attachmentMapper.toResponse(attachment)).thenReturn(response);

        AttachmentResponse result = attachmentService.upload(10L, 20L, 30L, file);

        assertThat(result.getFileName()).isEqualTo("spec.pdf");
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    void upload_shouldRejectWhenNotProjectMember() {
        MultipartFile file = new MockMultipartFile("file", "spec.pdf", "application/pdf", "data".getBytes());

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(project.getTeam());
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.upload(10L, 20L, 30L, file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void update_shouldRenameAttachment() {
        UpdateAttachmentRequest request = new UpdateAttachmentRequest();
        request.setFileName("new-name.pdf");
        AttachmentResponse response = AttachmentResponse.builder().id(40L).fileName("new-name.pdf").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(project.getTeam());
        when(attachmentRepository.findByIdAndProjectId(40L, 30L)).thenReturn(Optional.of(attachment));
        when(attachmentRepository.save(attachment)).thenReturn(attachment);
        when(attachmentMapper.toResponse(attachment)).thenReturn(response);

        AttachmentResponse result = attachmentService.update(10L, 20L, 30L, 40L, request);

        assertThat(result.getFileName()).isEqualTo("new-name.pdf");
        assertThat(attachment.getFileName()).isEqualTo("new-name.pdf");
    }

    @Test
    void delete_shouldRemoveAttachment() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(teamService).verifyCanManageProject(project.getTeam());
        when(attachmentRepository.findByIdAndProjectId(40L, 30L)).thenReturn(Optional.of(attachment));

        attachmentService.delete(10L, 20L, 30L, 40L);

        verify(fileStorageService).delete("projects/30/uuid_spec.pdf");
        verify(attachmentRepository).delete(attachment);
    }

}
