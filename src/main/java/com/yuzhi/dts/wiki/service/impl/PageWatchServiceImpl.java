package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.PageWatch;
import com.yuzhi.dts.wiki.repository.PageWatchRepository;
import com.yuzhi.dts.wiki.service.PageWatchService;
import com.yuzhi.dts.wiki.service.dto.PageWatchDTO;
import com.yuzhi.dts.wiki.service.mapper.PageWatchMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.PageWatch}.
 */
@Service
@Transactional
public class PageWatchServiceImpl implements PageWatchService {

    private static final Logger LOG = LoggerFactory.getLogger(PageWatchServiceImpl.class);

    private final PageWatchRepository pageWatchRepository;

    private final PageWatchMapper pageWatchMapper;

    public PageWatchServiceImpl(PageWatchRepository pageWatchRepository, PageWatchMapper pageWatchMapper) {
        this.pageWatchRepository = pageWatchRepository;
        this.pageWatchMapper = pageWatchMapper;
    }

    @Override
    public PageWatchDTO save(PageWatchDTO pageWatchDTO) {
        LOG.debug("Request to save PageWatch : {}", pageWatchDTO);
        PageWatch pageWatch = pageWatchMapper.toEntity(pageWatchDTO);
        pageWatch = pageWatchRepository.save(pageWatch);
        return pageWatchMapper.toDto(pageWatch);
    }

    @Override
    public PageWatchDTO update(PageWatchDTO pageWatchDTO) {
        LOG.debug("Request to update PageWatch : {}", pageWatchDTO);
        PageWatch pageWatch = pageWatchMapper.toEntity(pageWatchDTO);
        pageWatch = pageWatchRepository.save(pageWatch);
        return pageWatchMapper.toDto(pageWatch);
    }

    @Override
    public Optional<PageWatchDTO> partialUpdate(PageWatchDTO pageWatchDTO) {
        LOG.debug("Request to partially update PageWatch : {}", pageWatchDTO);

        return pageWatchRepository
            .findById(pageWatchDTO.getId())
            .map(existingPageWatch -> {
                pageWatchMapper.partialUpdate(existingPageWatch, pageWatchDTO);

                return existingPageWatch;
            })
            .map(pageWatchRepository::save)
            .map(pageWatchMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PageWatchDTO> findAll() {
        LOG.debug("Request to get all PageWatches");
        return pageWatchRepository.findAll().stream().map(pageWatchMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<PageWatchDTO> findAllWithEagerRelationships(Pageable pageable) {
        return pageWatchRepository.findAllWithEagerRelationships(pageable).map(pageWatchMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PageWatchDTO> findOne(Long id) {
        LOG.debug("Request to get PageWatch : {}", id);
        return pageWatchRepository.findOneWithEagerRelationships(id).map(pageWatchMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete PageWatch : {}", id);
        pageWatchRepository.deleteById(id);
    }
}
