package com.project.moviecachesim.service;

import com.project.moviecachesim.model.Movie;
import com.project.moviecachesim.model.MovieResponse;
import com.project.moviecachesim.repository.MovieRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MovieCacheService {

    @Autowired
    private MovieRepository movieRepository;

    private final Map<Long, Movie> cacheMap = new HashMap<>();
    private final PriorityQueue<Movie> minHeap;
    private final int CACHE_CAPACITY = 5;

    private long totalRequests = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;

    public MovieCacheService() {
        this.minHeap = new PriorityQueue<>(Comparator.comparingInt(Movie::getAccessCount));
    }

    // --- ID İLE GETİR ---
    public MovieResponse getMovie(Long movieId) {
        long startTime = System.nanoTime();
        totalRequests++;

        if (cacheMap.containsKey(movieId)) {
            return handleCacheHit(cacheMap.get(movieId), startTime);
        }

        cacheMisses++;
        Optional<Movie> dbMovieOpt = movieRepository.findById(movieId);
        return handleDbFetch(dbMovieOpt, startTime);
    }

    // --- LİSTELEME ---
    public List<Movie> getAllMoviesSorted() {
        List<Movie> all = movieRepository.findAll();
        all.sort((m1, m2) -> Integer.compare(m2.getAccessCount(), m1.getAccessCount()));
        return all;
    }

    public List<Movie> getCurrentCache() {
        List<Movie> list = new ArrayList<>(cacheMap.values());
        list.sort((m1, m2) -> Integer.compare(m2.getAccessCount(), m1.getAccessCount()));
        return list;
    }

    // --- YARDIMCI METODLAR ---
    private MovieResponse handleCacheHit(Movie movie, long startTime) {
        cacheHits++;
        movie.setAccessCount(movie.getAccessCount() + 1);
        minHeap.remove(movie);
        minHeap.add(movie);
        movieRepository.save(movie); // DB ile senkronize et

        long duration = (System.nanoTime() - startTime) / 1000;
        return new MovieResponse(movie, "CACHE HIT", true, duration);
    }

    private MovieResponse handleDbFetch(Optional<Movie> opt, long startTime) {
        if (opt.isEmpty()) return new MovieResponse(null, "Yok", false, 0);

        Movie movie = opt.get();
        movie.setAccessCount(movie.getAccessCount() + 1);
        movieRepository.save(movie);

        addToCache(movie);

        long duration = (System.nanoTime() - startTime) / 1000;
        return new MovieResponse(movie, "CACHE MISS", false, duration);
    }

    private void addToCache(Movie movie) {
        if (cacheMap.size() >= CACHE_CAPACITY) {
            Movie least = minHeap.poll();
            if (least != null) cacheMap.remove(least.getId());
        }
        cacheMap.put(movie.getId(), movie);
        minHeap.add(movie);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", totalRequests);
        stats.put("cacheHits", cacheHits);
        stats.put("cacheMisses", cacheMisses);
        return stats;
    }

    @PostConstruct
    public void initDatabase() {
        // Temiz başlangıç
        movieRepository.deleteAll();
        cacheMap.clear();
        minHeap.clear();

        List<Movie> movies = Arrays.asList(
                // 1. Inception
                new Movie("Inception", "Rüya içinde rüya", 148,
                        "https://image.tmdb.org/t/p/w500/ljsZTbVsrQSqZgWeep2B1QiDKuh.jpg"),

                // 2. Matrix
                new Movie("Matrix", "Simülasyon dünyası", 136,
                        "https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg"),

                // 3. Interstellar
                new Movie("Interstellar", "Uzay yolculuğu", 169,
                        "https://image.tmdb.org/t/p/w500/gEU2QniL6C8z1dY4kdNON4K6j9i.jpg"),

                // 4. Gora (Türkçe Poster bulamazsak uzay temalı bir tane koyuyoruz)
                new Movie("Gora", "Bir uzay filmi", 127,
                        "https://image.tmdb.org/t/p/w500/7ePASS0Qi3jB289RkB39D9d2w0Z.jpg"),

                // 5. Dark Knight
                new Movie("Dark Knight", "Batman vs Joker", 152,
                        "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg"),

                // 6. Fight Club
                new Movie("Fight Club", "Dövüş kulübü", 139,
                        "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg"),

                // 7. Pulp Fiction
                new Movie("Pulp Fiction", "Kült suç filmi", 154,
                        "https://image.tmdb.org/t/p/w500/tptX8zwEz9zzX9ZF777B64n85p8.jpg"),

                // 8. Gladiator
                new Movie("Gladiator", "Roma generali", 155,
                        "https://image.tmdb.org/t/p/w500/ty8TGRuvJLPUmAR1H1nRIsgwvim.jpg"),

                // 9. Avatar
                new Movie("Avatar", "Pandora gezegeni", 162,
                        "https://image.tmdb.org/t/p/w500/kyeqWdyUXW608qlYkRqosgbbJyK.jpg"),

                // 10. Titanic
                new Movie("Titanic", "Batan gemi", 195,
                        "https://image.tmdb.org/t/p/w500/9xjZS2rlVxm8SFx8kPC3aIGCOYQ.jpg"),

                // 11. Godfather
                new Movie("Godfather", "Mafya babası", 175,
                        "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg"),

                // 12. Lord of the Rings
                new Movie("Lord of the Rings", "Yüzüklerin efendisi", 201,
                        "https://image.tmdb.org/t/p/w500/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg"),

                // 13. Star Wars
                new Movie("Star Wars", "Güç seninle olsun", 121,
                        "https://image.tmdb.org/t/p/w500/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg"),

                // 14. Joker
                new Movie("Joker", "Toplumun dışlanmışı", 122,
                        "https://image.tmdb.org/t/p/w500/udDclJoHjfjb8Ekgsd4FDteOkCU.jpg"),

                // 15. Avengers
                new Movie("Avengers", "Süper kahramanlar", 181,
                        "https://image.tmdb.org/t/p/w500/RYMX2wcKCBAr24UyPD7xwmjaTn.jpg"),

                // 16. Toy Story
                new Movie("Toy Story", "Oyuncak hikayesi", 81,
                        "https://image.tmdb.org/t/p/w500/uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg"),

                // 17. Lion King
                new Movie("Lion King", "Aslan kral", 88,
                        "https://image.tmdb.org/t/p/w500/sKCr78MXSLixwmZ8DyJLrpMsdP5.jpg"),

                // 18. Forrest Gump
                new Movie("Forrest Gump", "Koş Forrest koş", 142,
                        "https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg"),

                // 19. Truman Show
                new Movie("Truman Show", "Her şey yalanmış", 103,
                        "https://image.tmdb.org/t/p/w500/cmSPq0jG53K84ZfQ32fXyL7C7wK.jpg"),

                // 20. Back to the Future
                new Movie("Back to the Future", "Zamanda yolculuk", 116,
                        "https://image.tmdb.org/t/p/w500/fNOH9f1aA7XRTzl1sA1xaU0MWDD.jpg"),

                // 21. Silence of the Lambs
                new Movie("Silence of the Lambs", "Kuzuların sessizliği", 118,
                        "https://image.tmdb.org/t/p/w500/rplLJ2hPcOQmkFhTqUte0MkEaO2.jpg"),

                // 22. Schindler's List
                new Movie("Schindlers List", "İkinci dünya savaşı", 195,
                        "https://image.tmdb.org/t/p/w500/sF1U4EUQS8YHUYjNl3pTXMYnljO.jpg"),

                // 23. Prestige
                new Movie("Prestige", "Sihirbazların savaşı", 130,
                        "https://image.tmdb.org/t/p/w500/tRNlZbgNCNOpLpbPEz5L8G8A0JN.jpg"),

                // 24. Memento
                new Movie("Memento", "Hafıza kaybı", 113,
                        "https://image.tmdb.org/t/p/w500/yuNs09hvpHVU1cbtCAk9z058WHb.jpg"),

                // 25. Seven
                new Movie("Seven", "Yedi günah", 127,
                        "https://image.tmdb.org/t/p/w500/6yoghtyTpznpBik8EngEmJskVUO.jpg")
        );

        movieRepository.saveAll(movies);
        System.out.println("✅ VERİTABANI: Gerçek film posterleri yüklendi!");
    }
}