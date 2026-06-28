package com.workmanagement.backend.integration.collaboration;

import com.workmanagement.backend.comment.dto.request.CreateCommentRequest;
import com.workmanagement.backend.comment.dto.request.UpdateCommentRequest;
import com.workmanagement.backend.integration.support.AbstractIntegrationTest;
import com.workmanagement.backend.integration.support.ProjectTestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CollaborationIntegrationTest extends AbstractIntegrationTest {

  @Test
  void commentAndAttachmentFlow_shouldWorkEndToEnd() throws Exception {
    ProjectTestContext context = setupPmProject(uniqueId());
    String token = context.tokens().accessToken();

    Long pbiId = createPbi(context, "Collaboration PBI");
    Long taskId = createPreparationTask(context, pbiId, "Task for comments");

    String commentsBase = "/api/v1/workspaces/%d/teams/%d/projects/%d/tasks/%d/comments"
        .formatted(context.workspaceId(), context.teamId(), context.projectId(), taskId);

    CreateCommentRequest parentRequest = new CreateCommentRequest();
    parentRequest.setContent("Parent comment for integration test");

    var parentResult = mockMvc.perform(post(commentsBase)
            .with(bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(parentRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andReturn();
    Long parentCommentId = readData(parentResult).get("id").asLong();

    CreateCommentRequest replyRequest = new CreateCommentRequest();
    replyRequest.setContent("Reply comment");
    replyRequest.setParentCommentId(parentCommentId);

    mockMvc.perform(post(commentsBase)
            .with(bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(replyRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.parentCommentId").value(parentCommentId));

    mockMvc.perform(get(commentsBase).with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(2));

    UpdateCommentRequest updateRequest = new UpdateCommentRequest();
    updateRequest.setContent("Updated parent comment");

    mockMvc.perform(put("%s/%d".formatted(commentsBase, parentCommentId))
            .with(bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("EDITED"))
        .andExpect(jsonPath("$.data.content").value("Updated parent comment"));

    String attachmentsBase = commentsBase + "/" + parentCommentId + "/attachments";
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "note.txt",
        MediaType.TEXT_PLAIN_VALUE,
        "integration attachment content".getBytes()
    );

    var uploadResult = mockMvc.perform(multipart(attachmentsBase)
            .file(file)
            .with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fileName").value("note.txt"))
        .andReturn();
    Long attachmentId = readData(uploadResult).get("id").asLong();

    mockMvc.perform(get(attachmentsBase).with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].id").value(attachmentId));

    mockMvc.perform(get("%s/%d/download".formatted(attachmentsBase, attachmentId))
            .with(bearer(token)))
        .andExpect(status().isOk());

    mockMvc.perform(delete("%s/%d".formatted(attachmentsBase, attachmentId))
            .with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    mockMvc.perform(get(attachmentsBase).with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(0));

    mockMvc.perform(delete("%s/%d".formatted(commentsBase, parentCommentId))
            .with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    mockMvc.perform(get(commentsBase).with(bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1));

    mockMvc.perform(get("%s/%d".formatted(commentsBase, parentCommentId))
            .with(bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }
}
