package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageDraft;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.PageDraftDTO;
import com.yuzhi.dts.wiki.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PageDraft} and its DTO {@link PageDraftDTO}.
 */
@Mapper(componentModel = "spring")
public interface PageDraftMapper extends EntityMapper<PageDraftDTO, PageDraft> {
    @Mapping(target = "page", source = "page", qualifiedByName = "pageId")
    @Mapping(target = "user", source = "user", qualifiedByName = "userLogin")
    PageDraftDTO toDto(PageDraft s);

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
