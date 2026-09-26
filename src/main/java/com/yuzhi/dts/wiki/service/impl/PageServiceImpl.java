package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.service.PageService;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.mapper.PageMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// DTS-WIKI: customized (entity named Page clashes with Spring Data Page):
// Spring's Page is fully qualified below, the import is intentionally absent.
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.Page}.
 */
@Service
@Transactional
public class PageServiceImpl implements PageService {

    private static final Logger LOG = LoggerFactory.getLogger(PageServiceImpl.class);

    private final PageRepository pageRepository;

    private final PageMapper pageMapper;

    public PageServiceImpl(PageRepository pageRepository, PageMapper pageMapper) {
        this.pageRepository = pageRepository;
        this.pageMapper = pageMapper;
    }

    @Override
    public PageDTO save(PageDTO pageDTO) {
        LOG.debug("Request to save Page : {}", pageDTO);
        Page page = pageMapper.toEntity(pageDTO);
        page = pageRepository.save(page);
        return pageMapper.toDto(page);
    }

    @Override
    public PageDTO update(PageDTO pageDTO) {
        LOG.debug("Request to update Page : {}", pageDTO);
        Page page = pageMapper.toEntity(pageDTO);
        page = pageRepository.save(page);
        return pageMapper.toDto(page);
    }

    @Override
    public Optional<PageDTO> partialUpdate(PageDTO pageDTO) {
        LOG.debug("Request to partially update Page : {}", pageDTO);

        return pageRepository
            .findById(pageDTO.getId())
            .map(existingPage -> {
                pageMapper.partialUpdate(existingPage, pageDTO);

                return existingPage;
            })
            .map(pageRepository::save)
            .map(pageMapper::toDto);
    }

    public org.springframework.data.domain.Page<PageDTO> findAllWithEagerRelationships(Pageable pageable) {
        return pageRepository.findAllWithEagerRelationships(pageable).map(pageMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PageDTO> findOne(Long id) {
        LOG.debug("Request to get Page : {}", id);
        return pageRepository.findOneWithEagerRelationships(id).map(pageMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Page : {}", id);
        pageRepository.deleteById(id);
    }
}
