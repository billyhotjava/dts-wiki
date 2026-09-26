package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.PageVersionDTO;
import com.yuzhi.dts.wiki.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PageVersion} and its DTO {@link PageVersionDTO}.
 */
@Mapper(componentModel = "spring")
public interface PageVersionMapper extends EntityMapper<PageVersionDTO, PageVersion> {
    @Mapping(target = "author", source = "author", qualifiedByName = "userLogin")
    @Mapping(target = "page", source = "page", qualifiedByName = "pageId")
    PageVersionDTO toDto(PageVersion s);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    @Named("pageId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PageDTO toDtoPageId(Page page);
}
