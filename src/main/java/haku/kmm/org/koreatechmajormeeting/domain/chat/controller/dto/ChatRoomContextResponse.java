package haku.kmm.org.koreatechmajormeeting.domain.chat.controller.dto;

import java.util.List;

public record ChatRoomContextResponse(
    Long roomId,
    Long postId,
    Long postWriterUserId,
    Long requesterUserId,
    String status,
    String otherUserName,
    String postIntroduction,
    List<ChatRoomPostMemberInfoResponse> postMembers
) {
}
