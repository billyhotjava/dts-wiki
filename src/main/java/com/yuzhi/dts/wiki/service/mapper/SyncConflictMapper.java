package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.SyncConflict;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.PageVersionDTO;
import com.yuzhi.dts.wiki.service.dto.SyncConflictDTO;
import com.yuzhi.dts.wiki.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SyncConflict} and its DTO {@link SyncConflictDTO}.
 */
@Mapper(componentModel = "spring")
public interface SyncConflictMapper extends EntityMapper<SyncConflictDTO, SyncConflict> {
    @Mapping(target = "page", source = "page", qualifiedByName = "pageTitle")
    @Mapping(target = "baseVersion", source = "baseVersion", qualifiedByName = "pageVersionId")
    @Mapping(target = "wikiVersion", source = "wikiVersion", qualifiedByName = "pageVersionId")
    @Mapping(target = "resolvedBy", source = "resolvedBy", qualifiedByName = "userLogin")
    SyncConflictDTO toDto(SyncConflict s);

    @Named("pageTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    PageDTO toDtoPageTitle(Page page);

    @Named("pageVersionId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PageVersionDTO toDtoPageVersionId(PageVersion pageVersion);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
