package com.uit.backend_cinema.unit_test;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import com.uit.backend_cinema.common.exception.GlobalExceptionHandler;
import com.uit.backend_cinema.modules.movies.api.controller.admin.AdminMovieController;
import com.uit.backend_cinema.modules.movies.api.mapper.MovieApiMapper;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;

class MovieDateValidationTest {
    final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminMovieController(
            mock(MovieService.class), mock(MovieApiMapper.class)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    String body(LocalDate release, String duration, String genres) {
        return "{\"releaseDate\":\"" + release + "\",\"duration\":" + duration + ",\"genreIdList\":" + genres + "}";
    }
    @Test void updateAcceptsPastReleaseButCreateRejectsIt() throws Exception {
        String payload = body(LocalDate.now().minusDays(1), "120", "[1]");
        mvc.perform(put("/api/v1/admin/movies/update/1").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/movies/create").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
    }
    @Test void createStillChecksDefaultConstraintsAndAllowsToday() throws Exception {
        mvc.perform(post("/api/v1/admin/movies/create").contentType(MediaType.APPLICATION_JSON)
                .content(body(LocalDate.now(), "120", "[1]"))).andExpect(status().isOk());
        for (String payload : new String[]{body(LocalDate.now(), "14", "[1]"),
                body(LocalDate.now(), "null", "[1]"), body(LocalDate.now(), "120", "[]")}) {
            mvc.perform(post("/api/v1/admin/movies/create").contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest());
        }
    }
}
