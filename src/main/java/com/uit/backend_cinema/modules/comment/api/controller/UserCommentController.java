package com.uit.backend_cinema.modules.comment.api.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.comment.api.dto.request.CreateCommentRequest;
import com.uit.backend_cinema.modules.comment.api.dto.response.CommentResponse;
import com.uit.backend_cinema.modules.comment.api.mapper.MovieCommentApiMapper;
import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;
import com.uit.backend_cinema.modules.comment.domain.service.MovieCommentService;

@RestController
@RequestMapping("/api/v1/comments")
public class UserCommentController {
    private final MovieCommentService commentService;
    private final MovieCommentApiMapper mapper;

    public UserCommentController(MovieCommentService commentService, MovieCommentApiMapper mapper) {
        this.commentService = commentService;
        this.mapper = mapper;
    }

    @PostMapping("/movies/{movieId}")
    public ResponseEntity<?> createComment(
            @PathVariable Long movieId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        MovieComment comment = commentService.createComment(user.getUserId(), movieId, request.getContent());
        CommentResponse response = mapper.toResponse(comment);
        return ResponseEntity.ok(ApiResponse.success(response, "Thêm bình luận thành công"));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        commentService.deleteComment(commentId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa bình luận thành công"));
    }
}
