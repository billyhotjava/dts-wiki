package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Label;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.service.dto.LabelDTO;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Label} and its DTO {@link LabelDTO}.
 */
@Mapper(componentModel = "spring")
public interface LabelMapper extends EntityMapper<LabelDTO, Label> {
    @Mapping(target = "pageses", source = "pageses", qualifiedByName = "pageIdSet")
    LabelDTO toDto(Label s);

    @Mapping(target = "pageses", ignore = true)
    @Mapping(target = "removePages", ignore = true)
    Label toEntity(LabelDTO labelDTO);

    @Named("pageId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PageDTO toDtoPageId(Page page);

    @Named("pageIdSet")
    default Set<PageDTO> toDtoPageIdSet(Set<Page> page) {
        return page.stream().map(this::toDtoPageId).collect(Collectors.toSet());
    }
}
