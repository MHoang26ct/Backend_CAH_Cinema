package com.uit.backend_cinema.unit_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;
import com.uit.backend_cinema.modules.comment.domain.repository.MovieCommentRepository;
import com.uit.backend_cinema.modules.comment.domain.service.MovieCommentService;

class MovieCommentServiceTest {

    @Test
    @DisplayName("Movie comment: Không cho phép bình luận nếu chưa xem phim (checked-in = 0)")
    void createCommentRejectsWhenNotWatched() {
        MovieCommentRepository repository = mock(MovieCommentRepository.class);
        MovieCommentService service = new MovieCommentService(repository);

        Long userId = 1L;
        Long movieId = 100L;
        String content = "Phim rat hay!";

        when(repository.countCheckedInBookingsByUserAndMovie(userId, movieId)).thenReturn(0L);

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> service.createComment(userId, movieId, content));
        
        assertEquals(ErrorCode.USER_NOT_CHECKED_IN, exception.getCode());
        assertEquals("Bạn cần xem phim trước khi đánh giá", exception.getMessage());
        verify(repository, never()).save(any(MovieComment.class));
    }

    @Test
    @DisplayName("Movie comment: Không cho phép bình luận nếu số lần bình luận vượt quá số lần xem phim")
    void createCommentRejectsWhenCommentLimitExceeded() {
        MovieCommentRepository repository = mock(MovieCommentRepository.class);
        MovieCommentService service = new MovieCommentService(repository);

        Long userId = 1L;
        Long movieId = 100L;
        String content = "Phim rat hay!";

        when(repository.countCheckedInBookingsByUserAndMovie(userId, movieId)).thenReturn(1L);
        when(repository.countCommentsByUserAndMovie(userId, movieId)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> service.createComment(userId, movieId, content));
        
        assertEquals(ErrorCode.COMMENT_LIMIT_EXCEEDED, exception.getCode());
        assertEquals("Bạn đã hết lượt bình luận cho phim này", exception.getMessage());
        verify(repository, never()).save(any(MovieComment.class));
    }

    @Test
    @DisplayName("Movie comment: Cho phép bình luận nếu số lần bình luận nhỏ hơn số lần xem phim")
    void createCommentSucceeds() {
        MovieCommentRepository repository = mock(MovieCommentRepository.class);
        MovieCommentService service = new MovieCommentService(repository);

        Long userId = 1L;
        Long movieId = 100L;
        String content = "Phim rat hay!";

        when(repository.countCheckedInBookingsByUserAndMovie(userId, movieId)).thenReturn(2L);
        when(repository.countCommentsByUserAndMovie(userId, movieId)).thenReturn(1L);
        
        MovieComment mockSaved = new MovieComment();
        mockSaved.setCommentId(10L);
        mockSaved.setUserId(userId);
        mockSaved.setMovieId(movieId);
        mockSaved.setContent(content);

        when(repository.save(any(MovieComment.class))).thenReturn(mockSaved);

        MovieComment result = service.createComment(userId, movieId, content);
        
        assertNotNull(result);
        assertEquals(10L, result.getCommentId());
        assertEquals(content, result.getContent());
        verify(repository).save(any(MovieComment.class));
    }

    @Test
    @DisplayName("Movie comment: Xóa bình luận thất bại nếu bình luận không tồn tại")
    void deleteCommentRejectsWhenNotFound() {
        MovieCommentRepository repository = mock(MovieCommentRepository.class);
        MovieCommentService service = new MovieCommentService(repository);

        Long commentId = 50L;
        Long userId = 1L;

        when(repository.findById(commentId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> service.deleteComment(commentId, userId));
        
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getCode());
        verify(repository, never()).save(any(MovieComment.class));
    }

    @Test
    @DisplayName("Movie comment: Xóa bình luận thất bại nếu người dùng không phải là chủ sở hữu")
    void deleteCommentRejectsWhenNotOwner() {
        MovieCommentRepository repository = mock(MovieCommentRepository.class);
        MovieCommentService service = new MovieCommentService(repository);

        Long commentId = 50L;
        Long userId = 1L;

        MovieComment comment = new MovieComment();
        comment.setCommentId(commentId);
        comment.setUserId(99L); // different user

        when(repository.findById(commentId)).thenReturn(Optional.of(comment));

        BusinessException exception = assertThrows(BusinessException.class, 
                () -> service.deleteComment(commentId, userId));
        
        assertEquals(ErrorCode.FORBIDDEN, exception.getCode());
        verify(repository, never()).save(any(MovieComment.class));
    }

    @Test
    @DisplayName("Movie comment: Xóa bình luận thành công")
    void deleteCommentSucceeds() {
        MovieCommentRepository repository = mock(MovieCommentRepository.class);
        MovieCommentService service = new MovieCommentService(repository);

        Long commentId = 50L;
        Long userId = 1L;

        MovieComment comment = new MovieComment();
        comment.setCommentId(commentId);
        comment.setUserId(userId);
        comment.setIsDeleted(false);

        when(repository.findById(commentId)).thenReturn(Optional.of(comment));
        when(repository.save(any(MovieComment.class))).thenReturn(comment);

        service.deleteComment(commentId, userId);
        
        assertEquals(true, comment.getIsDeleted());
        verify(repository).save(comment);
    }
}
