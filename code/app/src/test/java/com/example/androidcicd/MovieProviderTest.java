package com.example.androidcicd;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.androidcicd.movie.Movie;
import com.example.androidcicd.movie.MovieProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MovieProviderTest {

    @Mock
    private FirebaseFirestore mockFirestore;
    @Mock
    private CollectionReference mockMovieCollection;
    @Mock
    private DocumentReference mockDocRef;

    private MovieProvider movieProvider;

    @Before
    public void setUp() {
        // Initialize Mockito annotations
        MockitoAnnotations.openMocks(this);

        // Set up mock behavior
        when(mockFirestore.collection("movies")).thenReturn(mockMovieCollection);
        when(mockMovieCollection.document()).thenReturn(mockDocRef);
        when(mockMovieCollection.document(anyString())).thenReturn(mockDocRef);

        // Set the MovieProvider instance with the mock Firestore
        MovieProvider.setInstanceForTesting(mockFirestore);
        movieProvider = MovieProvider.getInstance(mockFirestore);
    }

    @Test
    public void testAddMovieSetsId() {
        // Arrange
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        when(mockDocRef.getId()).thenReturn("123");

        // Act
        movieProvider.addMovie(movie);

        // Assert
        assertEquals("Movie was not updated with correct ID.", "123", movie.getId());
        verify(mockDocRef).set(movie);
    }

    @Test
    public void testDeleteMovie() {
        // Arrange
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("123");

        // Act
        movieProvider.deleteMovie(movie);

        // Assert
        verify(mockMovieCollection).document("123");
        verify(mockDocRef).delete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateMovieShouldThrowErrorForDifferentIds() {
        // Arrange
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("1");
        when(mockDocRef.getId()).thenReturn("123");

        // Act
        movieProvider.updateMovie(movie, "Another Title", "Another Genre", 2026);

        // Assert: Expects IllegalArgumentException due to ID mismatch
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateMovieShouldThrowErrorForEmptyTitle() {
        // Arrange
        Movie movie = new Movie("Oppenheimer", "Thriller/Historical Drama", 2023);
        movie.setId("123");
        when(mockDocRef.getId()).thenReturn("123");

        // Act
        movieProvider.updateMovie(movie, "", "Another Genre", 2026);

        // Assert: Expects IllegalArgumentException due to empty title
    }
}