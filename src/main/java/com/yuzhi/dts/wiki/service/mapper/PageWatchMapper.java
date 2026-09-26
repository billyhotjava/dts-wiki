package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageWatch;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.PageWatchDTO;
import com.yuzhi.dts.wiki.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PageWatch} and its DTO {@link PageWatchDTO}.
 */
@Mapper(componentModel = "spring")
public interface PageWatchMapper extends EntityMapper<PageWatchDTO, PageWatch> {
    @Mapping(target = "page", source = "page", qualifiedByName = "pageId")
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    PageWatchDTO toDto(PageWatch s);

    @Named("pageId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PageDTO toDtoPageId(Page page);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
