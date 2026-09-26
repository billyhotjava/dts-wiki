package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.service.dto.SpaceDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Space} and its DTO {@link SpaceDTO}.
 */
@Mapper(componentModel = "spring")
public interface SpaceMapper extends EntityMapper<SpaceDTO, Space> {}
