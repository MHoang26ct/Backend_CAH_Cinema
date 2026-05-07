package com.uit.backend_cinema.modules.movies.infrastructure.persistence;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.movies.domain.entity.Genre;
import com.uit.backend_cinema.modules.movies.domain.repository.GenreRepository;
import com.uit.backend_cinema.modules.movies.infrastructure.mapper.GenreInfraMapper;
import com.uit.backend_cinema.modules.movies.infrastructure.repository.JpaGenreRepository;

@Repository
public class GenreRepositoryImpl implements GenreRepository {
    private final JpaGenreRepository jpaGenreRepository;
    private final GenreInfraMapper mapper;

    public GenreRepositoryImpl(JpaGenreRepository jpaGenreRepository, GenreInfraMapper mapper) {
        this.mapper = mapper;
        this.jpaGenreRepository = jpaGenreRepository;
    }

    @Override
    public List<Genre> findAll() {
        return jpaGenreRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Genre> findAllById(List<Long> genreIdList) {
        return jpaGenreRepository.findAllById(genreIdList)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
