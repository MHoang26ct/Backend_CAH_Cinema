package com.uit.backend_cinema.modules.cinema.infrastructure.persistence;

import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.repository.CinemaRepository;
import com.uit.backend_cinema.modules.cinema.infrastructure.mapper.CinemaInfraMapper;
import com.uit.backend_cinema.modules.cinema.infrastructure.repository.JpaCinemaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CinemaRepositoryImpl implements CinemaRepository {

    private final JpaCinemaRepository jpaRepository;
    private final CinemaInfraMapper mapper;

    public CinemaRepositoryImpl(JpaCinemaRepository jpaRepository, CinemaInfraMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Cinema> findById(long cinemaId) {
        return jpaRepository.findById(cinemaId).map(mapper::toDomain);
    }

    @Override
    public List<Cinema> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Cinema save(Cinema cinema) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(cinema)));
    }

    @Override
    public void delete(Cinema cinema) {
        jpaRepository.save(mapper.toEntity(cinema));
    }
}
