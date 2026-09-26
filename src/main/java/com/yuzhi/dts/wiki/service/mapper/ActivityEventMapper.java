package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.ActivityEvent;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.service.dto.ActivityEventDTO;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.SpaceDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ActivityEvent} and its DTO {@link ActivityEventDTO}.
 */
@Mapper(componentModel = "spring")
public interface ActivityEventMapper extends EntityMapper<ActivityEventDTO, ActivityEvent> {
    @Mapping(target = "space", source = "space", qualifiedByName = "spaceSlug")
    @Mapping(target = "page", source = "page", qualifiedByName = "pageId")
    ActivityEventDTO toDto(ActivityEvent s);

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
