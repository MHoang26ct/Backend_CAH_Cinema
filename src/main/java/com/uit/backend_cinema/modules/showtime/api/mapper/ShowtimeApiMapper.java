package com.uit.backend_cinema.modules.showtime.api.mapper;

import com.uit.backend_cinema.modules.showtime.api.dto.ShowtimeDetailDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.ShowtimeSummaryDTO;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShowtimeApiMapper {

    // MapStruct dùng .name() cho enum → String, cần dùng getValue() để ra "2D"/"3D"/"IMAX"
    @Mapping(target = "format", expression = "java(showtime.getFormat() != null ? showtime.getFormat().getValue() : null)")
    ShowtimeSummaryDTO toSummaryDto(Showtime showtime);

    @Mapping(target = "format", expression = "java(showtime.getFormat() != null ? showtime.getFormat().getValue() : null)")
    ShowtimeDetailDTO toDetailDto(Showtime showtime);
}
