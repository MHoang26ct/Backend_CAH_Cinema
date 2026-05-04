package com.uit.backend_cinema.modules.showtime.api.mapper;

import com.uit.backend_cinema.modules.showtime.api.dto.ShowtimeDetailDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.ShowtimeSummaryDTO;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface  ShowtimeApiMapper {
    ShowtimeSummaryDTO toSummaryDto(Showtime showtime);
    ShowtimeDetailDTO toDetailDto(Showtime showtime);
}
