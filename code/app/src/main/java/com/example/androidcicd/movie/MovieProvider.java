package com.example.androidcicd.movie;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class MovieProvider {
    private static MovieProvider movieProvider;
    private final ArrayList<Movie> movies;
    private final CollectionReference movieCollection;

    private MovieProvider(FirebaseFirestore firestore) {
        movies = new ArrayList<>();
        movieCollection = firestore.collection("movies");
    }

    public static void setInstanceForTesting(FirebaseFirestore mockFirestore) {
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public void listenForUpdates(final DataStatus dataStatus) {
        movieCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                dataStatus.onError(error.getMessage());
                return;
            }
            movies.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot item : snapshot) {
                    movies.add(item.toObject(Movie.class));
                }
                dataStatus.onDataUpdated();
            }
        });
    }

    public static MovieProvider getInstance(FirebaseFirestore firestore) {
        if (movieProvider == null)
            movieProvider = new MovieProvider(firestore);
        return movieProvider;
    }

    public ArrayList<Movie> getMovies() {
        return movies;
    }



    public Task<Void> addMovie(Movie movie) {
        // Check if a movie with the same title already exists
        return movieCollection.whereEqualTo("title", movie.getTitle())
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    QuerySnapshot result = task.getResult();
                    if (result.isEmpty()) {
                        // Title is unique, proceed to add the movie
                        DocumentReference docRef = movieCollection.document();
                        movie.setId(docRef.getId());
                        return docRef.set(movie);
                    } else {
                        // Duplicate title found
                        throw new IllegalArgumentException("Movie with this title already exists");
                    }
                });
    }

    public Task<Void> updateMovie(Movie movie, String title, String genre, int year) {
        String originalTitle = movie.getTitle();
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setYear(year);
        DocumentReference docRef = movieCollection.document(movie.getId());

        // Check if another movie (different ID) has the same title
        return movieCollection.whereEqualTo("title", title)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        if (!doc.getId().equals(movie.getId())) {
                            movie.setTitle(originalTitle); // Revert title on failure
                            throw new IllegalArgumentException("Another movie with this title already exists");
                        }
                    }
                    // Title is unique among other movies, proceed with update
                    return docRef.set(movie);
                });
    }

    public void deleteMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document(movie.getId());
        docRef.delete();
    }

    public boolean validMovie(Movie movie, DocumentReference docRef) {
        return movie.getId().equals(docRef.getId()) && !movie.getTitle().isEmpty() && !movie.getGenre().isEmpty() && movie.getYear() > 0;
    }
}
