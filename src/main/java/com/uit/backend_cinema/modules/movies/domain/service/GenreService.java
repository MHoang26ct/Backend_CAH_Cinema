package com.uit.backend_cinema.modules.movies.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.modules.movies.domain.entity.Genre;
import com.uit.backend_cinema.modules.movies.domain.repository.GenreRepository;

@Service
@Transactional(readOnly = true)
public class GenreService {
    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public List<Genre> findAll() {
        return genreRepository.findAll();
    }

    public List<Genre> findAllById(List<Long> genreIdList) {
        List<Genre> genres = genreRepository.findAllById(genreIdList);
        if (genres.size() != genreIdList.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều thể loại không tồn tại");
        }
        return genres;
    }
}
