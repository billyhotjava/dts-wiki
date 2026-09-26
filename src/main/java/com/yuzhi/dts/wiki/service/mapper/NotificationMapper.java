package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Notification;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.service.dto.NotificationDTO;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Notification} and its DTO {@link NotificationDTO}.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper extends EntityMapper<NotificationDTO, Notification> {
    @Mapping(target = "recipient", source = "recipient", qualifiedByName = "userLogin")
    @Mapping(target = "page", source = "page", qualifiedByName = "pageId")
    NotificationDTO toDto(Notification s);

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
