package co.onmind.hex4w.application.mappers;

import co.onmind.hex4w.application.dto.out.ScriptResultResponseDto;
import co.onmind.hex4w.domain.models.ScriptResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScriptingMapper {
    default ScriptResultResponseDto toResponseDto(ScriptResult result) {
        return new ScriptResultResponseDto(
            result.value(),
            result.stdout(),
            result.stderr()
        );
    }
}