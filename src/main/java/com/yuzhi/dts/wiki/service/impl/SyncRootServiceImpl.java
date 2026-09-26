package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.repository.SyncRootRepository;
import com.yuzhi.dts.wiki.service.SyncRootService;
import com.yuzhi.dts.wiki.service.dto.SyncRootDTO;
import com.yuzhi.dts.wiki.service.mapper.SyncRootMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.SyncRoot}.
 */
@Service
@Transactional
public class SyncRootServiceImpl implements SyncRootService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncRootServiceImpl.class);

    private final SyncRootRepository syncRootRepository;

    private final SyncRootMapper syncRootMapper;

    public SyncRootServiceImpl(SyncRootRepository syncRootRepository, SyncRootMapper syncRootMapper) {
        this.syncRootRepository = syncRootRepository;
        this.syncRootMapper = syncRootMapper;
    }

    @Override
    public SyncRootDTO save(SyncRootDTO syncRootDTO) {
        LOG.debug("Request to save SyncRoot : {}", syncRootDTO);
        SyncRoot syncRoot = syncRootMapper.toEntity(syncRootDTO);
        syncRoot = syncRootRepository.save(syncRoot);
        return syncRootMapper.toDto(syncRoot);
    }

    @Override
    public SyncRootDTO update(SyncRootDTO syncRootDTO) {
        LOG.debug("Request to update SyncRoot : {}", syncRootDTO);
        SyncRoot syncRoot = syncRootMapper.toEntity(syncRootDTO);
        syncRoot = syncRootRepository.save(syncRoot);
        return syncRootMapper.toDto(syncRoot);
    }

    @Override
    public Optional<SyncRootDTO> partialUpdate(SyncRootDTO syncRootDTO) {
        LOG.debug("Request to partially update SyncRoot : {}", syncRootDTO);

        return syncRootRepository
            .findById(syncRootDTO.getId())
            .map(existingSyncRoot -> {
                syncRootMapper.partialUpdate(existingSyncRoot, syncRootDTO);

                return existingSyncRoot;
            })
            .map(syncRootRepository::save)
            .map(syncRootMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncRootDTO> findAll() {
        LOG.debug("Request to get all SyncRoots");
        return syncRootRepository.findAll().stream().map(syncRootMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<SyncRootDTO> findAllWithEagerRelationships(Pageable pageable) {
        return syncRootRepository.findAllWithEagerRelationships(pageable).map(syncRootMapper::toDto);
    }

    /**
     *  Get all the syncRoots where SyncState is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<SyncRootDTO> findAllWhereSyncStateIsNull() {
        LOG.debug("Request to get all syncRoots where SyncState is null");
        return StreamSupport.stream(syncRootRepository.findAll().spliterator(), false)
            .filter(syncRoot -> syncRoot.getSyncState() == null)
            .map(syncRootMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SyncRootDTO> findOne(Long id) {
        LOG.debug("Request to get SyncRoot : {}", id);
        return syncRootRepository.findOneWithEagerRelationships(id).map(syncRootMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete SyncRoot : {}", id);
        syncRootRepository.deleteById(id);
    }
}
