package com.project.moviecachesim.controller;

import com.project.moviecachesim.model.Movie;
import com.project.moviecachesim.model.MovieResponse;
import com.project.moviecachesim.service.MovieCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MovieController {

    private final MovieCacheService service;

    public MovieController(MovieCacheService service) {
        this.service = service;
    }

    @GetMapping("/movies/{id}")
    public MovieResponse getMovie(@PathVariable Long id) {
        MovieResponse response = service.getMovie(id);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found: " + id);
        }
        return response;
    }

    @GetMapping("/movies/list")
    public List<Movie> getAllMovies() {
        return service.getAllMoviesSorted();
    }

    @GetMapping("/cache-content")
    public List<Movie> getCacheContent() {
        return service.getCurrentCache();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return service.getStats();
    }
}
