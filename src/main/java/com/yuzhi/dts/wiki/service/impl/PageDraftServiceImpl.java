package com.yuzhi.dts.wiki.service.impl;

import com.yuzhi.dts.wiki.domain.PageDraft;
import com.yuzhi.dts.wiki.repository.PageDraftRepository;
import com.yuzhi.dts.wiki.service.PageDraftService;
import com.yuzhi.dts.wiki.service.dto.PageDraftDTO;
import com.yuzhi.dts.wiki.service.mapper.PageDraftMapper;
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
 * Service Implementation for managing {@link com.yuzhi.dts.wiki.domain.PageDraft}.
 */
@Service
@Transactional
public class PageDraftServiceImpl implements PageDraftService {

    private static final Logger LOG = LoggerFactory.getLogger(PageDraftServiceImpl.class);

    private final PageDraftRepository pageDraftRepository;

    private final PageDraftMapper pageDraftMapper;

    public PageDraftServiceImpl(PageDraftRepository pageDraftRepository, PageDraftMapper pageDraftMapper) {
        this.pageDraftRepository = pageDraftRepository;
        this.pageDraftMapper = pageDraftMapper;
    }

    @Override
    public PageDraftDTO save(PageDraftDTO pageDraftDTO) {
        LOG.debug("Request to save PageDraft : {}", pageDraftDTO);
        PageDraft pageDraft = pageDraftMapper.toEntity(pageDraftDTO);
        pageDraft = pageDraftRepository.save(pageDraft);
        return pageDraftMapper.toDto(pageDraft);
    }

    @Override
    public PageDraftDTO update(PageDraftDTO pageDraftDTO) {
        LOG.debug("Request to update PageDraft : {}", pageDraftDTO);
        PageDraft pageDraft = pageDraftMapper.toEntity(pageDraftDTO);
        pageDraft = pageDraftRepository.save(pageDraft);
        return pageDraftMapper.toDto(pageDraft);
    }

    @Override
    public Optional<PageDraftDTO> partialUpdate(PageDraftDTO pageDraftDTO) {
        LOG.debug("Request to partially update PageDraft : {}", pageDraftDTO);

        return pageDraftRepository
            .findById(pageDraftDTO.getId())
            .map(existingPageDraft -> {
                pageDraftMapper.partialUpdate(existingPageDraft, pageDraftDTO);

                return existingPageDraft;
            })
            .map(pageDraftRepository::save)
            .map(pageDraftMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PageDraftDTO> findAll() {
        LOG.debug("Request to get all PageDrafts");
        return pageDraftRepository.findAll().stream().map(pageDraftMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<PageDraftDTO> findAllWithEagerRelationships(Pageable pageable) {
        return pageDraftRepository.findAllWithEagerRelationships(pageable).map(pageDraftMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PageDraftDTO> findOne(Long id) {
        LOG.debug("Request to get PageDraft : {}", id);
        return pageDraftRepository.findOneWithEagerRelationships(id).map(pageDraftMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete PageDraft : {}", id);
        pageDraftRepository.deleteById(id);
    }
}
