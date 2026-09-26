package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Attachment;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.service.dto.AttachmentDTO;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Attachment} and its DTO {@link AttachmentDTO}.
 */
@Mapper(componentModel = "spring")
public interface AttachmentMapper extends EntityMapper<AttachmentDTO, Attachment> {
    @Mapping(target = "page", source = "page", qualifiedByName = "pageId")
    AttachmentDTO toDto(Attachment s);

    @Named("pageId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PageDTO toDtoPageId(Page page);
}
