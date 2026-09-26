package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.SyncOutbox;
import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.service.SyncOutboxService;
import com.yuzhi.dts.wiki.service.dto.SyncOutboxDTO;
import com.yuzhi.dts.wiki.service.mapper.SyncOutboxMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.SyncOutbox}.
 */
@Service
@Transactional
public class SyncOutboxServiceImpl implements SyncOutboxService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncOutboxServiceImpl.class);

    private final SyncOutboxRepository syncOutboxRepository;

    private final SyncOutboxMapper syncOutboxMapper;

    public SyncOutboxServiceImpl(SyncOutboxRepository syncOutboxRepository, SyncOutboxMapper syncOutboxMapper) {
        this.syncOutboxRepository = syncOutboxRepository;
        this.syncOutboxMapper = syncOutboxMapper;
    }

    @Override
    public SyncOutboxDTO save(SyncOutboxDTO syncOutboxDTO) {
        LOG.debug("Request to save SyncOutbox : {}", syncOutboxDTO);
        SyncOutbox syncOutbox = syncOutboxMapper.toEntity(syncOutboxDTO);
        syncOutbox = syncOutboxRepository.save(syncOutbox);
        return syncOutboxMapper.toDto(syncOutbox);
    }

    @Override
    public SyncOutboxDTO update(SyncOutboxDTO syncOutboxDTO) {
        LOG.debug("Request to update SyncOutbox : {}", syncOutboxDTO);
        SyncOutbox syncOutbox = syncOutboxMapper.toEntity(syncOutboxDTO);
        syncOutbox = syncOutboxRepository.save(syncOutbox);
        return syncOutboxMapper.toDto(syncOutbox);
    }

    @Override
    public Optional<SyncOutboxDTO> partialUpdate(SyncOutboxDTO syncOutboxDTO) {
        LOG.debug("Request to partially update SyncOutbox : {}", syncOutboxDTO);

        return syncOutboxRepository
            .findById(syncOutboxDTO.getId())
            .map(existingSyncOutbox -> {
                syncOutboxMapper.partialUpdate(existingSyncOutbox, syncOutboxDTO);

                return existingSyncOutbox;
            })
            .map(syncOutboxRepository::save)
            .map(syncOutboxMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SyncOutboxDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all SyncOutboxes");
        return syncOutboxRepository.findAll(pageable).map(syncOutboxMapper::toDto);
    }

    public Page<SyncOutboxDTO> findAllWithEagerRelationships(Pageable pageable) {
        return syncOutboxRepository.findAllWithEagerRelationships(pageable).map(syncOutboxMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SyncOutboxDTO> findOne(Long id) {
        LOG.debug("Request to get SyncOutbox : {}", id);
        return syncOutboxRepository.findOneWithEagerRelationships(id).map(syncOutboxMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete SyncOutbox : {}", id);
        syncOutboxRepository.deleteById(id);
    }
}
