package com.project.moviecachesim.service;

import com.project.moviecachesim.model.Movie;
import com.project.moviecachesim.model.MovieResponse;
import com.project.moviecachesim.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MovieCacheServiceTest {

    private MovieRepository repository;
    private MovieCacheService service;

    @BeforeEach
    void setUp() {
        repository = mock(MovieRepository.class);
        when(repository.findById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (id > 10) return Optional.empty();
            Movie m = new Movie("Movie " + id, "desc", 100, "url");
            m.setId(id);
            return Optional.of(m);
        });
        when(repository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new MovieCacheService(repository, 3);
    }

    @Test
    void firstAccessIsMissSecondIsHit() {
        MovieResponse first = service.getMovie(1L);
        MovieResponse second = service.getMovie(1L);

        assertFalse(first.isCacheHit());
        assertTrue(second.isCacheHit());
        assertEquals(2, second.getMovie().getAccessCount());
    }

    @Test
    void unknownMovieReturnsNullAndIsNotCounted() {
        assertNull(service.getMovie(99L));
        assertEquals(0L, service.getStats().get("totalRequests"));
    }

    @Test
    void evictsLeastFrequentlyUsed() {
        service.getMovie(1L);
        service.getMovie(1L);
        service.getMovie(1L); // 1 is popular
        service.getMovie(2L);
        service.getMovie(2L);
        service.getMovie(3L); // 3 is the least frequently used
        service.getMovie(4L); // evicts 3

        assertEquals(Set.of(1L, 2L, 4L), cachedIds());
        assertEquals(1L, service.getStats().get("evictions"));
    }

    @Test
    void tieIsBrokenByLeastRecentlyUsed() {
        service.getMovie(1L);
        service.getMovie(2L);
        service.getMovie(3L);
        service.getMovie(4L); // all have count 1, so 1 (oldest) is evicted

        assertEquals(Set.of(2L, 3L, 4L), cachedIds());
    }

    @Test
    void statsTrackHitsMissesAndRatio() {
        service.getMovie(1L);
        service.getMovie(1L);
        service.getMovie(1L);
        service.getMovie(2L);

        Map<String, Object> stats = service.getStats();
        assertEquals(4L, stats.get("totalRequests"));
        assertEquals(2L, stats.get("cacheHits"));
        assertEquals(2L, stats.get("cacheMisses"));
        assertEquals(0.5, (double) stats.get("hitRatio"), 1e-9);
        assertEquals(3, stats.get("capacity"));
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new MovieCacheService(repository, 0));
    }

    private Set<Long> cachedIds() {
        return service.getCurrentCache().stream().map(Movie::getId).collect(Collectors.toSet());
    }
}
