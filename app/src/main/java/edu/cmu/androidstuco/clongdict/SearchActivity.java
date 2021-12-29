package edu.cmu.androidstuco.clongdict;

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

import edu.cmu.androidstuco.clongdict.databinding.ActivitySearchBinding;

public class SearchActivity extends AppCompatActivity {

    private static final String JARGON = "gnuershk";
    private ActivitySearchBinding binding;
    protected RecyclerView.LayoutManager srLayoutManager;
    protected SearchResultAdapter srAdapter;
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

        rView = (RecyclerView) findViewById(R.id.recView2);
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            handleIntent(intent);
            //doMySearch(query);
        }
        srLayoutManager = new LinearLayoutManager(this);
        rView.setLayoutManager(srLayoutManager);

        mgr = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        sView = (SearchView) findViewById(R.id.search_bar);
        sView.setSearchableInfo(mgr.getSearchableInfo(getComponentName()));
        sView.setIconifiedByDefault(false);
        sView.setSubmitButtonEnabled(true);
        sView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent i1 = (Intent) SearchActivity.this.getIntent().clone();
                i1.putExtra(SearchManager.QUERY, query);
                SearchActivity.this.onNewIntent(i1);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false; //onQueryTextSubmit(newText);
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
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // Toast.makeText(this,"Query: "+query, Toast.LENGTH_LONG).show();
            srAdapter = new SearchResultAdapter(/*FirebaseFirestore.getInstance(),
                    "huoxinde-jazk", // TODO you know the thing*/
                    query==null?null:query.toLowerCase(Locale.ROOT));
            rView.setAdapter(srAdapter);
        }
    }

    @Override
    public boolean onSearchRequested() {
        //pauseSomeStuff();
        Toast.makeText(this,"bruh", Toast.LENGTH_LONG).show();
        Bundle appData = new Bundle();
        appData.putBoolean(SearchActivity.JARGON, true);
        String q0 = (String) sView.getQuery();
        startSearch(q0, false, appData, false);
        return super.onSearchRequested();
    }
}