package com.uit.backend_cinema.modules.comment.api.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.comment.api.dto.response.CommentResponse;
import com.uit.backend_cinema.modules.comment.api.mapper.MovieCommentApiMapper;
import com.uit.backend_cinema.modules.comment.domain.service.MovieCommentService;

@RestController
@RequestMapping("/api/v1/public/comments")
public class OpenCommentController {
    private final MovieCommentService commentService;
    private final MovieCommentApiMapper mapper;

    public OpenCommentController(MovieCommentService commentService, MovieCommentApiMapper mapper) {
        this.commentService = commentService;
        this.mapper = mapper;
    }

    @GetMapping("/movies/{movieId}")
    public ResponseEntity<?> getComments(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<CommentResponse> comments = commentService.getCommentsByMovieId(movieId, pageable)
                .map(mapper::toResponse);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}
