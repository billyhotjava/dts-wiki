package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.ActivityEvent;
import com.yuzhi.dts.wiki.repository.ActivityEventRepository;
import com.yuzhi.dts.wiki.service.ActivityEventService;
import com.yuzhi.dts.wiki.service.dto.ActivityEventDTO;
import com.yuzhi.dts.wiki.service.mapper.ActivityEventMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.ActivityEvent}.
 */
@Service
@Transactional
public class ActivityEventServiceImpl implements ActivityEventService {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityEventServiceImpl.class);

    private final ActivityEventRepository activityEventRepository;

    private final ActivityEventMapper activityEventMapper;

    public ActivityEventServiceImpl(ActivityEventRepository activityEventRepository, ActivityEventMapper activityEventMapper) {
        this.activityEventRepository = activityEventRepository;
        this.activityEventMapper = activityEventMapper;
    }

    @Override
    public ActivityEventDTO save(ActivityEventDTO activityEventDTO) {
        LOG.debug("Request to save ActivityEvent : {}", activityEventDTO);
        ActivityEvent activityEvent = activityEventMapper.toEntity(activityEventDTO);
        activityEvent = activityEventRepository.save(activityEvent);
        return activityEventMapper.toDto(activityEvent);
    }

    @Override
    public ActivityEventDTO update(ActivityEventDTO activityEventDTO) {
        LOG.debug("Request to update ActivityEvent : {}", activityEventDTO);
        ActivityEvent activityEvent = activityEventMapper.toEntity(activityEventDTO);
        activityEvent = activityEventRepository.save(activityEvent);
        return activityEventMapper.toDto(activityEvent);
    }

    @Override
    public Optional<ActivityEventDTO> partialUpdate(ActivityEventDTO activityEventDTO) {
        LOG.debug("Request to partially update ActivityEvent : {}", activityEventDTO);

        return activityEventRepository
            .findById(activityEventDTO.getId())
            .map(existingActivityEvent -> {
                activityEventMapper.partialUpdate(existingActivityEvent, activityEventDTO);

                return existingActivityEvent;
            })
            .map(activityEventRepository::save)
            .map(activityEventMapper::toDto);
    }

    public Page<ActivityEventDTO> findAllWithEagerRelationships(Pageable pageable) {
        return activityEventRepository.findAllWithEagerRelationships(pageable).map(activityEventMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ActivityEventDTO> findOne(Long id) {
        LOG.debug("Request to get ActivityEvent : {}", id);
        return activityEventRepository.findOneWithEagerRelationships(id).map(activityEventMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete ActivityEvent : {}", id);
        activityEventRepository.deleteById(id);
    }
}
