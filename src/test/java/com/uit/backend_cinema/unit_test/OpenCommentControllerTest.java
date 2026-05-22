package com.uit.backend_cinema.unit_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.comment.api.controller.OpenCommentController;
import com.uit.backend_cinema.modules.comment.api.dto.response.CommentResponse;
import com.uit.backend_cinema.modules.comment.api.mapper.MovieCommentApiMapper;
import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;
import com.uit.backend_cinema.modules.comment.domain.service.MovieCommentService;

class OpenCommentControllerTest {

    @Test
    @DisplayName("Open comment controller: lấy danh sách comment của phim trả về HttpStatus OK và Slice CommentResponse")
    void getCommentsSucceeds() {
        MovieCommentService service = mock(MovieCommentService.class);
        MovieCommentApiMapper mapper = mock(MovieCommentApiMapper.class);
        OpenCommentController controller = new OpenCommentController(service, mapper);

        Long movieId = 1L;
        int page = 0;
        int size = 3;

        MovieComment comment = new MovieComment();
        comment.setCommentId(10L);
        comment.setMovieId(movieId);
        comment.setUserId(9999L);
        comment.setContent("Hay!");
        comment.setCreatedAt(LocalDateTime.now());

        Slice<MovieComment> sliceDomain = new SliceImpl<>(List.of(comment));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(service.getCommentsByMovieId(eq(movieId), any(Pageable.class))).thenReturn(sliceDomain);

        CommentResponse responseDTO = new CommentResponse();
        responseDTO.setCommentId(10L);
        responseDTO.setUserId(9999L);
        responseDTO.setContent("Hay!");
        responseDTO.setCreatedAt(comment.getCreatedAt());

        when(mapper.toResponse(comment)).thenReturn(responseDTO);

        ResponseEntity<?> responseEntity = controller.getComments(movieId, page, size);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());

        ApiResponse<?> apiResponse = (ApiResponse<?>) responseEntity.getBody();
        assertNotNull(apiResponse.getData());
        Slice<?> sliceResult = (Slice<?>) apiResponse.getData();
        assertEquals(1, sliceResult.getContent().size());
        assertEquals(responseDTO, sliceResult.getContent().get(0));

        verify(service).getCommentsByMovieId(eq(movieId), any(Pageable.class));
        verify(mapper).toResponse(comment);
    }
}
