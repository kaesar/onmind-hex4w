package co.onmind.hex4w.application.mappers;

import co.onmind.hex4w.application.dto.out.StoreItemResponseDto;
import co.onmind.hex4w.domain.models.StoreItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StoreMapper {
    default StoreItemResponseDto toResponseDto(StoreItem item) {
        return new StoreItemResponseDto(
            item.key(),
            item.size(),
            item.lastModified(),
            item.eTag()
        );
    }
}