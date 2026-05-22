package com.uit.backend_cinema.modules.comment.domain.service;

import java.util.Optional;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;
import com.uit.backend_cinema.modules.comment.domain.repository.MovieCommentRepository;

@Service
@Transactional(readOnly = true)
public class MovieCommentService {
    private final MovieCommentRepository commentRepository;

    public MovieCommentService(MovieCommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Slice<MovieComment> getCommentsByMovieId(Long movieId, Pageable pageable) {
        return commentRepository.findByMovieId(movieId, pageable);
    }

    @Transactional
    public MovieComment createComment(Long userId, Long movieId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("Nội dung bình luận không được để trống", ErrorCode.VALIDATION_FAILED);
        }

        long watchedCount = commentRepository.countCheckedInBookingsByUserAndMovie(userId, movieId);
        if (watchedCount == 0) {
            throw new BusinessException("Bạn cần xem phim trước khi đánh giá", ErrorCode.USER_NOT_CHECKED_IN);
        }

        long commentCount = commentRepository.countCommentsByUserAndMovie(userId, movieId);
        if (commentCount >= watchedCount) {
            throw new BusinessException("Bạn đã hết lượt bình luận cho phim này", ErrorCode.COMMENT_LIMIT_EXCEEDED);
        }

        MovieComment comment = new MovieComment();
        comment.setUserId(userId);
        comment.setMovieId(movieId);
        comment.setContent(content);

        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        MovieComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("Bình luận không tồn tại hoặc đã bị xóa", ErrorCode.RESOURCE_NOT_FOUND));

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền xóa bình luận này", ErrorCode.FORBIDDEN);
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }
}
