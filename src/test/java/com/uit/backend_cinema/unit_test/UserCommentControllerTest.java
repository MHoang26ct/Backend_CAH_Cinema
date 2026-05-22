package com.uit.backend_cinema.unit_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.comment.api.controller.UserCommentController;
import com.uit.backend_cinema.modules.comment.api.dto.request.CreateCommentRequest;
import com.uit.backend_cinema.modules.comment.api.dto.response.CommentResponse;
import com.uit.backend_cinema.modules.comment.api.mapper.MovieCommentApiMapper;
import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;
import com.uit.backend_cinema.modules.comment.domain.service.MovieCommentService;

class UserCommentControllerTest {

    @Test
    @DisplayName("User comment controller: Tạo bình luận thành công trả về HttpStatus OK và CommentResponse")
    void createCommentSucceeds() {
        MovieCommentService service = mock(MovieCommentService.class);
        MovieCommentApiMapper mapper = mock(MovieCommentApiMapper.class);
        UserCommentController controller = new UserCommentController(service, mapper);

        Long movieId = 1L;
        Long userId = 9999L;
        String content = "Phim rat hay!";

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent(content);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);

        MovieComment comment = new MovieComment();
        comment.setCommentId(10L);
        comment.setMovieId(movieId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());

        when(service.createComment(eq(userId), eq(movieId), eq(content))).thenReturn(comment);

        CommentResponse responseDTO = new CommentResponse();
        responseDTO.setCommentId(10L);
        responseDTO.setUserId(userId);
        responseDTO.setContent(content);
        responseDTO.setCreatedAt(comment.getCreatedAt());

        when(mapper.toResponse(comment)).thenReturn(responseDTO);

        ResponseEntity<?> responseEntity = controller.createComment(movieId, request, userDetails);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());

        ApiResponse<?> apiResponse = (ApiResponse<?>) responseEntity.getBody();
        assertNotNull(apiResponse.getData());
        assertEquals("Thêm bình luận thành công", apiResponse.getMessage());
        assertEquals(responseDTO, apiResponse.getData());

        verify(service).createComment(eq(userId), eq(movieId), eq(content));
        verify(mapper).toResponse(comment);
    }

    @Test
    @DisplayName("User comment controller: Xóa bình luận thành công trả về HttpStatus OK")
    void deleteCommentSucceeds() {
        MovieCommentService service = mock(MovieCommentService.class);
        MovieCommentApiMapper mapper = mock(MovieCommentApiMapper.class);
        UserCommentController controller = new UserCommentController(service, mapper);

        Long commentId = 10L;
        Long userId = 9999L;

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);

        ResponseEntity<?> responseEntity = controller.deleteComment(commentId, userDetails);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());

        ApiResponse<?> apiResponse = (ApiResponse<?>) responseEntity.getBody();
        assertEquals("Xóa bình luận thành công", apiResponse.getMessage());

        verify(service).deleteComment(eq(commentId), eq(userId));
    }
}
