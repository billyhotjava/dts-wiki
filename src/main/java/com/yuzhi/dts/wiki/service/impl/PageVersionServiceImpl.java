package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.service.PageVersionService;
import com.yuzhi.dts.wiki.service.dto.PageVersionDTO;
import com.yuzhi.dts.wiki.service.mapper.PageVersionMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.PageVersion}.
 */
@Service
@Transactional
public class PageVersionServiceImpl implements PageVersionService {

    private static final Logger LOG = LoggerFactory.getLogger(PageVersionServiceImpl.class);

    private final PageVersionRepository pageVersionRepository;

    private final PageVersionMapper pageVersionMapper;

    public PageVersionServiceImpl(PageVersionRepository pageVersionRepository, PageVersionMapper pageVersionMapper) {
        this.pageVersionRepository = pageVersionRepository;
        this.pageVersionMapper = pageVersionMapper;
    }

    @Override
    public PageVersionDTO save(PageVersionDTO pageVersionDTO) {
        LOG.debug("Request to save PageVersion : {}", pageVersionDTO);
        PageVersion pageVersion = pageVersionMapper.toEntity(pageVersionDTO);
        pageVersion = pageVersionRepository.save(pageVersion);
        return pageVersionMapper.toDto(pageVersion);
    }

    @Override
    public PageVersionDTO update(PageVersionDTO pageVersionDTO) {
        LOG.debug("Request to update PageVersion : {}", pageVersionDTO);
        PageVersion pageVersion = pageVersionMapper.toEntity(pageVersionDTO);
        pageVersion = pageVersionRepository.save(pageVersion);
        return pageVersionMapper.toDto(pageVersion);
    }

    @Override
    public Optional<PageVersionDTO> partialUpdate(PageVersionDTO pageVersionDTO) {
        LOG.debug("Request to partially update PageVersion : {}", pageVersionDTO);

        return pageVersionRepository
            .findById(pageVersionDTO.getId())
            .map(existingPageVersion -> {
                pageVersionMapper.partialUpdate(existingPageVersion, pageVersionDTO);

                return existingPageVersion;
            })
            .map(pageVersionRepository::save)
            .map(pageVersionMapper::toDto);
    }

    public Page<PageVersionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return pageVersionRepository.findAllWithEagerRelationships(pageable).map(pageVersionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PageVersionDTO> findOne(Long id) {
        LOG.debug("Request to get PageVersion : {}", id);
        return pageVersionRepository.findOneWithEagerRelationships(id).map(pageVersionMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete PageVersion : {}", id);
        pageVersionRepository.deleteById(id);
    }
}
