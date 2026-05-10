package com.uit.backend_cinema.modules.voucher.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.domain.repository.VoucherRepository;
import com.uit.backend_cinema.modules.voucher.infrastructure.mapper.VoucherInfraMapper;
import com.uit.backend_cinema.modules.voucher.infrastructure.repository.JpaVoucherRepository;

@Repository
public class VoucherRepositoryImpl implements VoucherRepository {
    private final JpaVoucherRepository jpaVoucherRepository;
    private final VoucherInfraMapper voucherInfraMapper;

    public VoucherRepositoryImpl(JpaVoucherRepository jpaVoucherRepository, VoucherInfraMapper voucherInfraMapper) {
        this.jpaVoucherRepository = jpaVoucherRepository;
        this.voucherInfraMapper = voucherInfraMapper;
    }

    @Override
    public Optional<Voucher> findById(long voucherId) {
        return jpaVoucherRepository.findById(voucherId)
                .map(voucherInfraMapper::toDomain);
    }

    @Override
    public Boolean existsByCode(String code) {
        return jpaVoucherRepository.existsByCode(code);
    }

    @Override
    public Slice<Voucher> findAllForAdmin(Pageable pageable) {
        return jpaVoucherRepository.findAllBy(pageable).map(voucherInfraMapper::toDomain);
    }

    @Override
    public int consumeVoucherAtomically(Long voucherId, LocalDateTime now) {
        return jpaVoucherRepository.consumeVoucherAtomically(voucherId, now);
    }

    @Override
    public int releaseVoucherAtomically(Long voucherId) {
        return jpaVoucherRepository.releaseVoucherAtomically(voucherId);
    }

    @Override
    public List<Voucher> findAllForUser(LocalDateTime now) {
        return jpaVoucherRepository.findAllForUser(now).stream()
                .map(voucherInfraMapper::toDomain)
                .toList();
    }

    @Override
    public Voucher save(Voucher voucher) {
        return voucherInfraMapper.toDomain(jpaVoucherRepository.save(voucherInfraMapper.toInfra(voucher)));
    }

    @Override
    public void delete(Voucher voucher) {
        jpaVoucherRepository.save(voucherInfraMapper.toInfra(voucher));
    }
}
