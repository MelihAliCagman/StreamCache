package com.project.moviecachesim.service;

import com.project.moviecachesim.model.Movie;
import com.project.moviecachesim.model.MovieResponse;
import com.project.moviecachesim.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Read-through cache with an LFU (Least Frequently Used) eviction policy.
 * Ties between equally popular movies are broken by recency (least recently used goes first).
 * All public methods are synchronized so the cache map and the eviction heap never diverge.
 */
@Service
public class MovieCacheService {

    private static class CacheEntry {
        final Movie movie;
        long lastUsed;

        CacheEntry(Movie movie, long lastUsed) {
            this.movie = movie;
            this.lastUsed = lastUsed;
        }
    }

    private final MovieRepository movieRepository;
    private final int capacity;

    private final Map<Long, CacheEntry> cacheMap = new HashMap<>();
    private final PriorityQueue<CacheEntry> minHeap = new PriorityQueue<>(
            Comparator.<CacheEntry>comparingInt(e -> e.movie.getAccessCount())
                    .thenComparingLong(e -> e.lastUsed));

    private long clock = 0;
    private long totalRequests = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long evictions = 0;

    public MovieCacheService(MovieRepository movieRepository,
                             @Value("${cache.capacity:5}") int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("cache.capacity must be at least 1");
        }
        this.movieRepository = movieRepository;
        this.capacity = capacity;
    }

    /** Returns the movie from the cache if present, otherwise loads it from the DB and caches it. */
    public synchronized MovieResponse getMovie(Long movieId) {
        long startTime = System.nanoTime();

        CacheEntry entry = cacheMap.get(movieId);
        if (entry != null) {
            totalRequests++;
            cacheHits++;
            recordAccess(entry);
            return new MovieResponse(entry.movie, "CACHE HIT", true, elapsedMicros(startTime));
        }

        Optional<Movie> dbMovie = movieRepository.findById(movieId);
        if (dbMovie.isEmpty()) {
            return null;
        }

        totalRequests++;
        cacheMisses++;
        Movie movie = dbMovie.get();
        movie.setAccessCount(movie.getAccessCount() + 1);
        movieRepository.save(movie);
        addToCache(movie);
        return new MovieResponse(movie, "CACHE MISS", false, elapsedMicros(startTime));
    }

    public List<Movie> getAllMoviesSorted() {
        List<Movie> all = movieRepository.findAll();
        all.sort(Comparator.comparingInt(Movie::getAccessCount).reversed());
        return all;
    }

    public synchronized List<Movie> getCurrentCache() {
        List<Movie> list = new ArrayList<>();
        cacheMap.values().forEach(e -> list.add(e.movie));
        list.sort(Comparator.comparingInt(Movie::getAccessCount).reversed());
        return list;
    }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRequests", totalRequests);
        stats.put("cacheHits", cacheHits);
        stats.put("cacheMisses", cacheMisses);
        stats.put("evictions", evictions);
        stats.put("capacity", capacity);
        stats.put("cacheSize", cacheMap.size());
        stats.put("hitRatio", totalRequests == 0 ? 0.0 : (double) cacheHits / totalRequests);
        return stats;
    }

    private void recordAccess(CacheEntry entry) {
        minHeap.remove(entry);
        entry.movie.setAccessCount(entry.movie.getAccessCount() + 1);
        entry.lastUsed = ++clock;
        minHeap.add(entry);
        movieRepository.save(entry.movie); // keep the DB in sync
    }

    private void addToCache(Movie movie) {
        if (cacheMap.size() >= capacity) {
            CacheEntry least = minHeap.poll();
            if (least != null) {
                cacheMap.remove(least.movie.getId());
                evictions++;
            }
        }
        CacheEntry entry = new CacheEntry(movie, ++clock);
        cacheMap.put(movie.getId(), entry);
        minHeap.add(entry);
    }

    private static long elapsedMicros(long startNanos) {
        return (System.nanoTime() - startNanos) / 1000;
    }
}
