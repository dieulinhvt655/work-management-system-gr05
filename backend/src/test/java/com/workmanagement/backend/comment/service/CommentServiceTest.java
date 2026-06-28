package com.workmanagement.backend.comment.service;

import com.workmanagement.backend.attachment.service.AttachmentService;
import com.workmanagement.backend.comment.dto.request.CreateCommentRequest;
import com.workmanagement.backend.comment.dto.request.UpdateCommentRequest;
import com.workmanagement.backend.comment.dto.response.CommentResponse;
import com.workmanagement.backend.comment.entity.Comment;
import com.workmanagement.backend.comment.mapper.CommentMapper;
import com.workmanagement.backend.comment.repository.CommentRepository;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.CommentStatus;
import com.workmanagement.backend.common.enums.CommonStatus;
import com.workmanagement.backend.common.enums.MemberStatus;
import com.workmanagement.backend.common.enums.ProjectStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.util.SecurityUtils;
import com.workmanagement.backend.productbacklog.entity.ProductBacklog;
import com.workmanagement.backend.productbacklog.entity.ProductBacklogItem;
import com.workmanagement.backend.project.entity.Project;
import com.workmanagement.backend.project.entity.ProjectMember;
import com.workmanagement.backend.project.repository.ProjectMemberRepository;
import com.workmanagement.backend.project.service.ProjectService;
import com.workmanagement.backend.task.entity.Task;
import com.workmanagement.backend.task.repository.TaskRepository;
import com.workmanagement.backend.team.entity.Team;
import com.workmanagement.backend.team.entity.TeamMember;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.workspace.entity.Workspace;
import com.workmanagement.backend.workspace.entity.WorkspaceMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private CommentService commentService;

    private Project project;
    private Task task;
    private ProjectMember author;
    private ProjectMember otherMember;
    private Comment comment;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = Mockito.mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(2L);

        Workspace workspace = Workspace.builder().id(10L).status(CommonStatus.ACTIVE).build();
        Team team = Team.builder().id(20L).workspace(workspace).status(CommonStatus.ACTIVE).build();
        project = Project.builder().id(30L).team(team).status(ProjectStatus.ACTIVE).build();

        ProductBacklog backlog = ProductBacklog.builder().id(50L).project(project).build();
        ProductBacklogItem pbi = ProductBacklogItem.builder().id(60L).backlog(backlog).build();
        task = Task.builder().id(70L).title("Implement login").pbi(pbi).build();

        User authorUser = User.builder().id(2L).fullName("Dev").build();
        User otherUser = User.builder().id(3L).fullName("Other").build();
        WorkspaceMember authorWs = WorkspaceMember.builder().id(5L).user(authorUser).build();
        WorkspaceMember otherWs = WorkspaceMember.builder().id(6L).user(otherUser).build();
        TeamMember authorTm = TeamMember.builder().id(7L).workspaceMember(authorWs).build();
        TeamMember otherTm = TeamMember.builder().id(8L).workspaceMember(otherWs).build();
        author = ProjectMember.builder().id(11L).teamMember(authorTm).status(MemberStatus.ACTIVE).build();
        otherMember = ProjectMember.builder().id(12L).teamMember(otherTm).status(MemberStatus.ACTIVE).build();

        comment = Comment.builder()
                .id(100L)
                .task(task)
                .authorMember(author)
                .content("Looks good")
                .status(CommentStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_shouldSaveComment() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Looks good");

        CommentResponse response = CommentResponse.builder().id(100L).content("Looks good").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(taskRepository.findByIdAndPbi_Backlog_Project_Id(70L, 30L)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.of(author));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toResponse(comment)).thenReturn(response);

        CommentResponse result = commentService.create(10L, 20L, 30L, 70L, request);

        assertThat(result.getContent()).isEqualTo("Looks good");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void create_shouldRejectWhenNotProjectMember() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Looks good");

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(taskRepository.findByIdAndPbi_Backlog_Project_Id(70L, 30L)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndTeamMember_WorkspaceMember_User_IdAndStatus(
                30L, 2L, MemberStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(10L, 20L, 30L, 70L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
    }

    @Test
    void findByTask_shouldReturnVisibleComments() {
        CommentResponse response = CommentResponse.builder().id(100L).content("Looks good").build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(taskRepository.findByIdAndPbi_Backlog_Project_Id(70L, 30L)).thenReturn(Optional.of(task));
        when(commentRepository.findByTaskIdAndStatusInOrderByCreatedAtAsc(eq(70L), any()))
                .thenReturn(List.of(comment));
        when(attachmentService.findResponsesByCommentIds(List.of(100L))).thenReturn(List.of());
        when(commentMapper.toResponse(comment, List.of())).thenReturn(response);

        List<CommentResponse> result = commentService.findByTask(10L, 20L, 30L, 70L);

        assertThat(result).hasSize(1);
    }

    @Test
    void update_shouldAllowAuthor() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated content");

        CommentResponse response = CommentResponse.builder()
                .id(100L)
                .content("Updated content")
                .status(CommentStatus.EDITED)
                .build();

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(commentRepository.findByIdAndTaskId(100L, 70L)).thenReturn(Optional.of(comment));
        when(projectService.isActiveProjectManager(project)).thenReturn(false);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toResponse(comment)).thenReturn(response);

        CommentResponse result = commentService.update(10L, 20L, 30L, 70L, 100L, request);

        assertThat(result.getStatus()).isEqualTo(CommentStatus.EDITED);
        assertThat(comment.getContent()).isEqualTo("Updated content");
    }

    @Test
    void update_shouldRejectNonAuthorNonPm() {
        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated content");
        comment.setAuthorMember(otherMember);

        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(commentRepository.findByIdAndTaskId(100L, 70L)).thenReturn(Optional.of(comment));
        when(projectService.isActiveProjectManager(project)).thenReturn(false);

        assertThatThrownBy(() -> commentService.update(10L, 20L, 30L, 70L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_ACCESS_DENIED);
    }

    @Test
    void delete_shouldSoftDeleteComment() {
        when(projectService.getProject(10L, 20L, 30L)).thenReturn(project);
        doNothing().when(projectService).verifyProjectAccess(project);
        when(commentRepository.findByIdAndTaskId(100L, 70L)).thenReturn(Optional.of(comment));
        when(projectService.isActiveProjectManager(project)).thenReturn(false);

        commentService.delete(10L, 20L, 30L, 70L, 100L);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
        assertThat(comment.getContent()).isEqualTo("[Đã xóa]");
        verify(commentRepository).save(comment);
    }

}
