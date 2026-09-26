package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncOutbox;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.SpaceDTO;
import com.yuzhi.dts.wiki.service.dto.SyncOutboxDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SyncOutbox} and its DTO {@link SyncOutboxDTO}.
 */
@Mapper(componentModel = "spring")
public interface SyncOutboxMapper extends EntityMapper<SyncOutboxDTO, SyncOutbox> {
    @Mapping(target = "space", source = "space", qualifiedByName = "spaceSlug")
    @Mapping(target = "page", source = "page", qualifiedByName = "pageId")
    SyncOutboxDTO toDto(SyncOutbox s);

    @Named("spaceSlug")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "slug", source = "slug")
    SpaceDTO toDtoSpaceSlug(Space space);

    @Named("pageId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PageDTO toDtoPageId(Page page);
}
