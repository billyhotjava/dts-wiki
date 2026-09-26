package com.yuzhi.dts.wiki.service.mapper;

import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.domain.SyncState;
import com.yuzhi.dts.wiki.service.dto.SyncRootDTO;
import com.yuzhi.dts.wiki.service.dto.SyncStateDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SyncState} and its DTO {@link SyncStateDTO}.
 */
@Mapper(componentModel = "spring")
public interface SyncStateMapper extends EntityMapper<SyncStateDTO, SyncState> {
    @Mapping(target = "syncRoot", source = "syncRoot", qualifiedByName = "syncRootId")
    SyncStateDTO toDto(SyncState s);

    @Named("syncRootId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    SyncRootDTO toDtoSyncRootId(SyncRoot syncRoot);
}
