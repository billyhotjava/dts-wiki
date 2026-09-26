package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.SpaceDTO;
import com.yuzhi.dts.wiki.service.dto.SyncRootDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SyncRoot} and its DTO {@link SyncRootDTO}.
 */
@Mapper(componentModel = "spring")
public interface SyncRootMapper extends EntityMapper<SyncRootDTO, SyncRoot> {
    @Mapping(target = "mountPage", source = "mountPage", qualifiedByName = "pageTitle")
    @Mapping(target = "space", source = "space", qualifiedByName = "spaceSlug")
    SyncRootDTO toDto(SyncRoot s);

    @Named("pageTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    PageDTO toDtoPageTitle(Page page);

    @Named("spaceSlug")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "slug", source = "slug")
    SpaceDTO toDtoSpaceSlug(Space space);
}
