package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Label;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.service.dto.LabelDTO;
import com.yuzhi.dts.wiki.service.dto.PageDTO;
import com.yuzhi.dts.wiki.service.dto.PageVersionDTO;
import com.yuzhi.dts.wiki.service.dto.SpaceDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Page} and its DTO {@link PageDTO}.
 */
@Mapper(componentModel = "spring")
public interface PageMapper extends EntityMapper<PageDTO, Page> {
    @Mapping(target = "currentVersion", source = "currentVersion", qualifiedByName = "pageVersionId")
    @Mapping(target = "labelses", source = "labelses", qualifiedByName = "labelNameSet")
    @Mapping(target = "space", source = "space", qualifiedByName = "spaceSlug")
    @Mapping(target = "parent", source = "parent", qualifiedByName = "pageTitle")
    PageDTO toDto(Page s);

    @Mapping(target = "removeLabels", ignore = true)
    Page toEntity(PageDTO pageDTO);

    @Named("pageTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    PageDTO toDtoPageTitle(Page page);

    @Named("pageVersionId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PageVersionDTO toDtoPageVersionId(PageVersion pageVersion);

    @Named("labelName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    LabelDTO toDtoLabelName(Label label);

    @Named("labelNameSet")
    default Set<LabelDTO> toDtoLabelNameSet(Set<Label> label) {
        return label.stream().map(this::toDtoLabelName).collect(Collectors.toSet());
    }

    @Named("spaceSlug")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "slug", source = "slug")
    SpaceDTO toDtoSpaceSlug(Space space);
}
