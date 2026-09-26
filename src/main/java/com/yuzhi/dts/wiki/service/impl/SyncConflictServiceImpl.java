package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.SyncConflict;
import com.yuzhi.dts.wiki.repository.SyncConflictRepository;
import com.yuzhi.dts.wiki.service.SyncConflictService;
import com.yuzhi.dts.wiki.service.dto.SyncConflictDTO;
import com.yuzhi.dts.wiki.service.mapper.SyncConflictMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.SyncConflict}.
 */
@Service
@Transactional
public class SyncConflictServiceImpl implements SyncConflictService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncConflictServiceImpl.class);

    private final SyncConflictRepository syncConflictRepository;

    private final SyncConflictMapper syncConflictMapper;

    public SyncConflictServiceImpl(SyncConflictRepository syncConflictRepository, SyncConflictMapper syncConflictMapper) {
        this.syncConflictRepository = syncConflictRepository;
        this.syncConflictMapper = syncConflictMapper;
    }

    @Override
    public SyncConflictDTO save(SyncConflictDTO syncConflictDTO) {
        LOG.debug("Request to save SyncConflict : {}", syncConflictDTO);
        SyncConflict syncConflict = syncConflictMapper.toEntity(syncConflictDTO);
        syncConflict = syncConflictRepository.save(syncConflict);
        return syncConflictMapper.toDto(syncConflict);
    }

    @Override
    public SyncConflictDTO update(SyncConflictDTO syncConflictDTO) {
        LOG.debug("Request to update SyncConflict : {}", syncConflictDTO);
        SyncConflict syncConflict = syncConflictMapper.toEntity(syncConflictDTO);
        syncConflict = syncConflictRepository.save(syncConflict);
        return syncConflictMapper.toDto(syncConflict);
    }

    @Override
    public Optional<SyncConflictDTO> partialUpdate(SyncConflictDTO syncConflictDTO) {
        LOG.debug("Request to partially update SyncConflict : {}", syncConflictDTO);

        return syncConflictRepository
            .findById(syncConflictDTO.getId())
            .map(existingSyncConflict -> {
                syncConflictMapper.partialUpdate(existingSyncConflict, syncConflictDTO);

                return existingSyncConflict;
            })
            .map(syncConflictRepository::save)
            .map(syncConflictMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SyncConflictDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all SyncConflicts");
        return syncConflictRepository.findAll(pageable).map(syncConflictMapper::toDto);
    }

    public Page<SyncConflictDTO> findAllWithEagerRelationships(Pageable pageable) {
        return syncConflictRepository.findAllWithEagerRelationships(pageable).map(syncConflictMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SyncConflictDTO> findOne(Long id) {
        LOG.debug("Request to get SyncConflict : {}", id);
        return syncConflictRepository.findOneWithEagerRelationships(id).map(syncConflictMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete SyncConflict : {}", id);
        syncConflictRepository.deleteById(id);
    }
}
