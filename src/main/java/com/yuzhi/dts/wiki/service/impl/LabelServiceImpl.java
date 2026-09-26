package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.Label;
import com.yuzhi.dts.wiki.repository.LabelRepository;
import com.yuzhi.dts.wiki.service.LabelService;
import com.yuzhi.dts.wiki.service.dto.LabelDTO;
import com.yuzhi.dts.wiki.service.mapper.LabelMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.Label}.
 */
@Service
@Transactional
public class LabelServiceImpl implements LabelService {

    private static final Logger LOG = LoggerFactory.getLogger(LabelServiceImpl.class);

    private final LabelRepository labelRepository;

    private final LabelMapper labelMapper;

    public LabelServiceImpl(LabelRepository labelRepository, LabelMapper labelMapper) {
        this.labelRepository = labelRepository;
        this.labelMapper = labelMapper;
    }

    @Override
    public LabelDTO save(LabelDTO labelDTO) {
        LOG.debug("Request to save Label : {}", labelDTO);
        Label label = labelMapper.toEntity(labelDTO);
        label = labelRepository.save(label);
        return labelMapper.toDto(label);
    }

    @Override
    public LabelDTO update(LabelDTO labelDTO) {
        LOG.debug("Request to update Label : {}", labelDTO);
        Label label = labelMapper.toEntity(labelDTO);
        label = labelRepository.save(label);
        return labelMapper.toDto(label);
    }

    @Override
    public Optional<LabelDTO> partialUpdate(LabelDTO labelDTO) {
        LOG.debug("Request to partially update Label : {}", labelDTO);

        return labelRepository
            .findById(labelDTO.getId())
            .map(existingLabel -> {
                labelMapper.partialUpdate(existingLabel, labelDTO);

                return existingLabel;
            })
            .map(labelRepository::save)
            .map(labelMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelDTO> findAll() {
        LOG.debug("Request to get all Labels");
        return labelRepository.findAll().stream().map(labelMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LabelDTO> findOne(Long id) {
        LOG.debug("Request to get Label : {}", id);
        return labelRepository.findById(id).map(labelMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Label : {}", id);
        labelRepository.deleteById(id);
    }
}
