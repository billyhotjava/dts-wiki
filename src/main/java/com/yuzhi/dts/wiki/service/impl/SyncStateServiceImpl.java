package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.SyncState;
import com.yuzhi.dts.wiki.repository.SyncStateRepository;
import com.yuzhi.dts.wiki.service.SyncStateService;
import com.yuzhi.dts.wiki.service.dto.SyncStateDTO;
import com.yuzhi.dts.wiki.service.mapper.SyncStateMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.SyncState}.
 */
@Service
@Transactional
public class SyncStateServiceImpl implements SyncStateService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncStateServiceImpl.class);

    private final SyncStateRepository syncStateRepository;

    private final SyncStateMapper syncStateMapper;

    public SyncStateServiceImpl(SyncStateRepository syncStateRepository, SyncStateMapper syncStateMapper) {
        this.syncStateRepository = syncStateRepository;
        this.syncStateMapper = syncStateMapper;
    }

    @Override
    public SyncStateDTO save(SyncStateDTO syncStateDTO) {
        LOG.debug("Request to save SyncState : {}", syncStateDTO);
        SyncState syncState = syncStateMapper.toEntity(syncStateDTO);
        syncState = syncStateRepository.save(syncState);
        return syncStateMapper.toDto(syncState);
    }

    @Override
    public SyncStateDTO update(SyncStateDTO syncStateDTO) {
        LOG.debug("Request to update SyncState : {}", syncStateDTO);
        SyncState syncState = syncStateMapper.toEntity(syncStateDTO);
        syncState = syncStateRepository.save(syncState);
        return syncStateMapper.toDto(syncState);
    }

    @Override
    public Optional<SyncStateDTO> partialUpdate(SyncStateDTO syncStateDTO) {
        LOG.debug("Request to partially update SyncState : {}", syncStateDTO);

        return syncStateRepository
            .findById(syncStateDTO.getId())
            .map(existingSyncState -> {
                syncStateMapper.partialUpdate(existingSyncState, syncStateDTO);

                return existingSyncState;
            })
            .map(syncStateRepository::save)
            .map(syncStateMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncStateDTO> findAll() {
        LOG.debug("Request to get all SyncStates");
        return syncStateRepository.findAll().stream().map(syncStateMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SyncStateDTO> findOne(Long id) {
        LOG.debug("Request to get SyncState : {}", id);
        return syncStateRepository.findById(id).map(syncStateMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete SyncState : {}", id);
        syncStateRepository.deleteById(id);
    }
}
