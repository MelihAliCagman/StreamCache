package com.project.moviecachesim.config;

import com.project.moviecachesim.model.Movie;
import com.project.moviecachesim.repository.MovieRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the database with sample movies on first start. Existing data (and view counts) is kept.
 */
@Component
public class MovieDataInitializer implements CommandLineRunner {

    private static final String POSTER = "https://image.tmdb.org/t/p/w500/";

    private final MovieRepository movieRepository;

    public MovieDataInitializer(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public void run(String... args) {
        if (movieRepository.count() > 0) {
            return;
        }

        movieRepository.saveAll(List.of(
                movie("Inception", "Rüya içinde rüya", 148, "ljsZTbVsrQSqZgWeep2B1QiDKuh.jpg"),
                movie("Matrix", "Simülasyon dünyası", 136, "f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg"),
                movie("Interstellar", "Uzay yolculuğu", 169, "gEU2QniL6C8z1dY4kdNON4K6j9i.jpg"),
                movie("Gora", "Bir uzay filmi", 127, "7ePASS0Qi3jB289RkB39D9d2w0Z.jpg"),
                movie("Dark Knight", "Batman vs Joker", 152, "qJ2tW6WMUDux911r6m7haRef0WH.jpg"),
                movie("Fight Club", "Dövüş kulübü", 139, "pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg"),
                movie("Pulp Fiction", "Kült suç filmi", 154, "tptX8zwEz9zzX9ZF777B64n85p8.jpg"),
                movie("Gladiator", "Roma generali", 155, "ty8TGRuvJLPUmAR1H1nRIsgwvim.jpg"),
                movie("Avatar", "Pandora gezegeni", 162, "kyeqWdyUXW608qlYkRqosgbbJyK.jpg"),
                movie("Titanic", "Batan gemi", 195, "9xjZS2rlVxm8SFx8kPC3aIGCOYQ.jpg"),
                movie("Godfather", "Mafya babası", 175, "3bhkrj58Vtu7enYsRolD1fZdja1.jpg"),
                movie("Lord of the Rings", "Yüzüklerin efendisi", 201, "6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg"),
                movie("Star Wars", "Güç seninle olsun", 121, "6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg"),
                movie("Joker", "Toplumun dışlanmışı", 122, "udDclJoHjfjb8Ekgsd4FDteOkCU.jpg"),
                movie("Avengers", "Süper kahramanlar", 181, "RYMX2wcKCBAr24UyPD7xwmjaTn.jpg"),
                movie("Toy Story", "Oyuncak hikayesi", 81, "uXDfjJbdP4ijW5hWSBrPrlKpxab.jpg"),
                movie("Lion King", "Aslan kral", 88, "sKCr78MXSLixwmZ8DyJLrpMsdP5.jpg"),
                movie("Forrest Gump", "Koş Forrest koş", 142, "arw2vcBveWOVZr6pxd9XTd1TdQa.jpg"),
                movie("Truman Show", "Her şey yalanmış", 103, "cmSPq0jG53K84ZfQ32fXyL7C7wK.jpg"),
                movie("Back to the Future", "Zamanda yolculuk", 116, "fNOH9f1aA7XRTzl1sA1xaU0MWDD.jpg"),
                movie("Silence of the Lambs", "Kuzuların sessizliği", 118, "rplLJ2hPcOQmkFhTqUte0MkEaO2.jpg"),
                movie("Schindlers List", "İkinci dünya savaşı", 195, "sF1U4EUQS8YHUYjNl3pTXMYnljO.jpg"),
                movie("Prestige", "Sihirbazların savaşı", 130, "tRNlZbgNCNOpLpbPEz5L8G8A0JN.jpg"),
                movie("Memento", "Hafıza kaybı", 113, "yuNs09hvpHVU1cbtCAk9z058WHb.jpg"),
                movie("Seven", "Yedi günah", 127, "6yoghtyTpznpBik8EngEmJskVUO.jpg")
        ));
    }

    private static Movie movie(String title, String description, double duration, String poster) {
        return new Movie(title, description, duration, POSTER + poster);
    }
}
