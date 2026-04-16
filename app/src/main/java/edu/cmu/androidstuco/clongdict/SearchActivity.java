package edu.cmu.androidstuco.clongdict;

import android.app.SearchableInfo;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.Toast;

import java.util.Locale;
import java.util.Objects;

import edu.cmu.androidstuco.clongdict.databinding.ActivitySearchBinding;

public class SearchActivity extends AppCompatActivity {

    // tf is this for
    private static final String JARGON = "gnuershk";

    private ActivitySearchBinding binding;
    protected RecyclerView.LayoutManager srLayoutManager;
    protected SearchResultAdapterV2 srAdapter;
    private SearchManager mgr;
    private SearchView sView;
    private RecyclerView rView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i0 = new Intent(SearchActivity.this, EditActivity.class);
                startActivity(i0);
            }
        });

        rView = findViewById(R.id.recView2);
        srLayoutManager = new LinearLayoutManager(this);
        rView.setLayoutManager(srLayoutManager);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            handleIntent(intent);
        }

        mgr = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        // Use view binding: activity findViewById can miss views nested under Toolbar here.
        sView = binding.searchBar;
        if (mgr != null) {
            SearchableInfo si = mgr.getSearchableInfo(getComponentName());
            if (si != null) {
                try {
                    sView.setSearchableInfo(si);
                } catch (RuntimeException ignored) {
                    // AppCompat SearchView + searchable metadata can throw on some configs;
                    // OnQueryTextListener + handleIntent still drive search.
                }
            }
        }
        sView.setIconifiedByDefault(false);
        sView.setSubmitButtonEnabled(true);
        sView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent i1 = (Intent) SearchActivity.this.getIntent().clone();
                i1.setAction(Intent.ACTION_SEARCH);
                i1.putExtra(SearchManager.QUERY, query);
                SearchActivity.this.onNewIntent(i1);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Toast.makeText(this,"bruh", Toast.LENGTH_SHORT).show();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            String qNorm = query == null ? null : query.toLowerCase(Locale.ROOT);
            String langPath = ConWord.lang;
            boolean pathChanged = srAdapter == null
                    || !Objects.equals(langPath, srAdapter.getBackingCollectionPath());
            if (pathChanged) {
                if (langPath != null) {
                    //Toast.makeText(this,"langPath: "+langPath, Toast.LENGTH_SHORT).show();
                    srAdapter = new SearchResultAdapterV2(
                            FirebaseFirestore.getInstance(),
                            langPath,
                            qNorm);
                    //Toast.makeText(this,"srAdapter created successfully", Toast.LENGTH_SHORT).show();
                } else {
                    srAdapter = new SearchResultAdapterV2(qNorm);
                }
                rView.setAdapter(srAdapter);
            } else {
                srAdapter.setQuery(qNorm);
            }
        }
    }

    @Override
    public boolean onSearchRequested() {
        //pauseSomeStuff();
        Toast.makeText(this,"bruh", Toast.LENGTH_LONG).show();
        Bundle appData = new Bundle();
        appData.putBoolean(SearchActivity.JARGON, true);
        // SearchView#getQuery() is CharSequence (often Editable), not String — casting throws ClassCastException.
        CharSequence qSeq = sView.getQuery();
        String q0 = qSeq != null ? qSeq.toString() : null;
        startSearch(q0, false, appData, false);
        return super.onSearchRequested();
    }
}