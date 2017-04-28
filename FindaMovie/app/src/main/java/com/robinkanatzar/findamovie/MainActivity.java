package com.robinkanatzar.findamovie;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.robinkanatzar.findamovie.api.RestClient;
import com.robinkanatzar.findamovie.api.SearchResponse;
import com.robinkanatzar.findamovie.recyclerview.Movie;
import com.robinkanatzar.findamovie.recyclerview.MovieAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {

    // API Documentation for The Movie DB
    // https://developers.themoviedb.org/3/search/search-people

    @BindView(R.id.et_query) EditText mQuery;
    @BindView(R.id.rv_results) RecyclerView mResults;
    @BindView(R.id.progress_spinner) ProgressBar mProgressSpinner;

    private String mQueryString;
    private List<Movie> movieList = new ArrayList<>();
    private MovieAdapter movieAdapter;
    private Subscription subscription = Subscriptions.empty();
    private Integer mPageNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Timber.i("Inside MainActivity onCreate");
        Log.d("RCK", "inside onCreate");

        movieAdapter = new MovieAdapter(movieList);
        mResults.setLayoutManager(new LinearLayoutManager(this));
        mResults.setAdapter(movieAdapter);
    }

    @OnClick(R.id.btn_search) void search() {

        movieList.clear();
        dismissKeyboard();

        mQueryString = mQuery.getText().toString().replace(" ", "_");
        if (mQueryString == "" || mQueryString.isEmpty() || mQueryString == null) {
            Toast.makeText(this, "Please enter a city.", Toast.LENGTH_SHORT).show();
        } else {
            mProgressSpinner.setVisibility(View.VISIBLE);

            rx.Observable<SearchResponse> observable = new RestClient().getService().searchForMovie(getString(R.string.API_KEY), mPageNumber, mQueryString);

            subscription = observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<SearchResponse>() {

                        String title;
                        String description;
                        String iconUrl;
                        int totalResults;
                        int currentPage;
                        int totalPages;

                        @Override
                        public void onCompleted() {
                            movieAdapter.notifyDataSetChanged();
                            mProgressSpinner.setVisibility(GONE);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e(e);
                        }

                        @Override
                        public void onNext(SearchResponse searchResponse) {

                            if(searchResponse.getTotalResults() != null) {
                                totalResults = searchResponse.getTotalResults();
                                currentPage = searchResponse.getPage();
                                totalPages = searchResponse.getTotalPages();

                                for (int i = 0; i < 19; i++) {
                                    if (searchResponse.getResults().get(i).getOriginalTitle() != null) {
                                        title = searchResponse.getResults().get(i).getOriginalTitle().toString();
                                        Timber.d("i = " + i + " " + title);
                                    } else {
                                        title = "";
                                    }

                                    if(searchResponse.getResults().get(i).getOverview() != null) {
                                        description = searchResponse.getResults().get(i).getOverview().toString();
                                        Timber.d("i = " + i + " " + description);
                                    } else {
                                        description = "";
                                    }

                                    if (searchResponse.getResults().get(i).getPosterPath() != null) {
                                        iconUrl = "http://image.tmdb.org/t/p/w500" + searchResponse.getResults().get(i).getPosterPath().toString();
                                        Timber.d("i = " + i + " " + iconUrl);
                                    } else {
                                        iconUrl = getString(R.string.default_movie_image);
                                    }

                                    addMovieToList(title, description, iconUrl);
                                }
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.no_results), Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
        }
    }

    private void dismissKeyboard() {
        View view = this.getCurrentFocus();

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void addMovieToList(String title, String description, String iconUrl) {
        Movie movie = new Movie(title, description, iconUrl);
        movieList.add(movie);
    }
}