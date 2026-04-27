package edu.cmu.androidstuco.clongdict;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import edu.cmu.androidstuco.clongdict.databinding.ActivityMainBinding;
import edu.cmu.androidstuco.clongdict.ui.login.LoginActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private HashMap<String,String> langs;
    public FirebaseFirestore db;
    /** Set by [SecondFragment] before [FragmentManager.popBackStack] so [FirstFragment] can restore scroll. */
    public int pendingDictionaryScrollPos = -1;
    private AppBarConfiguration mAppBarConfiguration;

    // I may hold on to this for future use
    private AppBarConfiguration appBarConfiguration;


    @Nullable
    private EntryFragment findVisibleSecondFragment() {
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f instanceof EntryFragment && f.isVisible()) {
                return (EntryFragment) f;
            }
        }
        return null;
    }

    private void navigateBackToListAfterEntryRemoved() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            fm.beginTransaction()
                    .replace(R.id.fragment_container_view_tag, BrowseFragment.class, null)
                    .commit();
        }
    }

    private void startEditWithWordLookup(@NonNull Intent editIntent, @NonNull String rawLemma) {
        if (ConWord.lang == null) {
            Toast.makeText(this, "No language selected", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection(ConWord.lang).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(MainActivity.this,
                        "Could not load entries to open editor.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            for (QueryDocumentSnapshot doc : task.getResult()) {
                Object w = doc.getData().get("word");
                if (w != null && rawLemma.equals(w.toString())) {
                    editIntent.putExtra("id", doc.getId());
                    startActivity(editIntent);
                    return;
                }
            }
            Toast.makeText(MainActivity.this,
                    "No matching entry for this lemma.",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void confirmDeleteCurrentEntry() {
        EntryFragment second = findVisibleSecondFragment();
        if (second == null || second.getArguments() == null) {
            Toast.makeText(this, "No entry to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ConWord.lang == null) {
            Toast.makeText(this, "No language selected", Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle args = second.getArguments();
        String entryId = args.getString("entryId");
        if (entryId != null && !entryId.isEmpty()) {
            String label = args.getString("word");
            if (label == null) label = entryId;
            Snackbar.make(findViewById(R.id.fragment_container_view_tag),
                            "Deletion is irreversable, do you want to delete «" + label + "»?",
                            Snackbar.LENGTH_LONG)
                    .setAction("CONFIRM", v -> db.collection(ConWord.lang).document(entryId).delete()
                            .addOnSuccessListener(unused -> {
                                navigateBackToListAfterEntryRemoved();
                                Snackbar.make(findViewById(R.id.fragment_container_view_tag),
                                        "Deletion Successful.", Snackbar.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Snackbar.make(
                                    findViewById(R.id.fragment_container_view_tag),
                                    "Deletion Failed.", Snackbar.LENGTH_SHORT).show()))
                    .setActionTextColor(0xc0ff0000)
                    .show();
            return;
        }
        String rawLemma = args.getString("word");
        if (rawLemma == null) {
            Toast.makeText(this, "No entry identifier", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection(ConWord.lang).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(MainActivity.this,
                        "Could not load entries to delete.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            for (QueryDocumentSnapshot doc : task.getResult()) {
                Object w = doc.getData().get("word");
                if (w != null && rawLemma.equals(w.toString())) {
                    String wordLabel = w.toString();
                    Snackbar.make(findViewById(R.id.fragment_container_view_tag),
                                    "Deletion is irreversable, do you want to delete «" + wordLabel + "»?",
                                    Snackbar.LENGTH_LONG)
                            .setAction("CONFIRM", v -> db.collection(ConWord.lang).document(doc.getId()).delete()
                                    .addOnSuccessListener(unused -> {
                                        navigateBackToListAfterEntryRemoved();
                                        Snackbar.make(findViewById(R.id.fragment_container_view_tag),
                                                "Deletion Successful.", Snackbar.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Snackbar.make(
                                            findViewById(R.id.fragment_container_view_tag),
                                            "Deletion Failed.", Snackbar.LENGTH_SHORT).show()))
                            .setActionTextColor(0xc0ff0000)
                            .show();
                    return;
                }
            }
            Toast.makeText(MainActivity.this,
                    "No matching entry to delete.",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private static Typeface clongUiTypeface(Context context) {
        Typeface base = ResourcesCompat.getFont(context, R.font.noto_sans_clong);
        if (base != null) {
            return Typeface.create(base, Typeface.NORMAL);
        }
        return Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.contentMain.toolbar);
        /*
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
         */
        // TODO DrawerLayout drawer = binding.contentMain.drawerLayout;
        //NavigationView navigationView = binding.navView;
        /*
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

         */
        /*
        NavController navController = Navigation.findNavController(this, R.id.fragment_container_view_tag);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
         */

        // Authorize firebase instance
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            // naturally this won't be hardcoded forever
            /*
            mAuth.signInWithEmailAndPassword("ccrawfor@andrew.cmu.edu","PASSWORD CHANGED")
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Toast.makeText(MainActivity.this, "createUserWithEmail:success", Toast.LENGTH_SHORT).show();
                                FirebaseUser user = mAuth.getCurrentUser();
                            } else {
                                // If sign in fails, display a message to the user.
                                //Toast.makeText(this, "createUserWithEmail:failure", Toast.LENGTH_SHORT);
                                Toast.makeText(MainActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
             */
            Intent lo = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(lo);
            return;
        }

        // Initialize db
        db = FirebaseFirestore.getInstance();
        langs = new HashMap<>();
        // Language metadata is stored in firebase
        // not much is done with this now, but I plan to expand it so users can switch between languages
        db.collection("languages")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int i = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //Toast.makeText(MainActivity.this,document.getId() + " => " + document.getData(), Toast.LENGTH_LONG).show();
                                langs.put((String) document.getData().get("path"),(String) document.getData().get("Name"));
                                // Populate navdrawer menu
                                Menu drawer = binding.navView.getMenu();
                                drawer.add(R.id.langs_menu, ((String) document.getData().get("path")).hashCode(),
                                            i, (String) document.getData().get("Name"))
                                          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                              @Override
                                              public boolean onMenuItemClick(MenuItem item) {
                                                  loadLanguage(
                                                          (String) document.getData().get("path"),
                                                          (String) document.getData().get("Name"),
                                                          (CharSequence) document.getData().get("alphabet"),
                                                          (CharSequence) document.getData().get("ignored")
                                                          );
                                                  return true;
                                              }
                                          });

                                // TODO make variable
                                if (ConWord.lang != null && ConWord.lang.equals((String) document.getData().get("path"))) {
                                    ConWord.alphabet = (CharSequence) document.getData().get("alphabet");
                                    ConWord.ignored = (CharSequence) document.getData().get("ignored");
                                    ConWord.clongTypeface = clongUiTypeface(MainActivity.this);
                                    drawer.findItem(((String) document.getData().get("path")).hashCode()).setChecked(true).setEnabled(false);
                                }
                                i++;
                            }
                            if (ConWord.lang != null && langs.containsKey(ConWord.lang)) {
                                binding.contentMain.toolbar.setTitle(langs.get(ConWord.lang));
                            }
                        } else {
                            System.err.println("Error getting documents.");
                            task.getException().printStackTrace();
                        }
                    }
                });

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i0 = new Intent(MainActivity.this, EditActivity.class);
                EntryFragment second = findVisibleSecondFragment();
                if (second != null && second.getArguments() != null) {
                    Bundle args = second.getArguments();
                    String entryId = args.getString("entryId");
                    if (entryId != null && !entryId.isEmpty()) {
                        i0.putExtra("id", entryId);
                        startActivity(i0);
                        return;
                    }
                    String rawWord = args.getString("word");
                    if (rawWord != null) {
                        startEditWithWordLookup(i0, rawWord);
                        return;
                    }
                }
                startActivity(i0);
            }
        });

        FragmentManager fm = this.getSupportFragmentManager();
        if (getIntent() != null && getIntent().getBooleanExtra("display_mode", false)) {
            Bundle b0 = new Bundle();
            b0.putString("word",getIntent().getStringExtra("word"));
            b0.putString("pron",getIntent().getStringExtra("pron"));
            b0.putString("lexcat",getIntent().getStringExtra("lexcat"));
            b0.putString("def" ,getIntent().getStringExtra("def"));
            b0.putString("etym",getIntent().getStringExtra("etym"));
            EntryFragment snd = new EntryFragment();
            snd.setArguments(b0);
            ((FloatingActionButton) findViewById(R.id.fab)).setImageResource(android.R.drawable.ic_menu_edit); // Pencil, ic_menu_edit
            fm.beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragment_container_view_tag,snd,null)
                .commit();
            return;
        }

        if (ConWord.lang != null) {
            fm.beginTransaction().setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view_tag, BrowseFragment.class, null)
                    .addToBackStack("dict").commit();
        }
        else {
            fm.beginTransaction().setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view_tag, HomeFragment.class, null)
                    .addToBackStack("dict").commit();
        }
    }

    public void loadLanguage(String path, String name, CharSequence alphabet, CharSequence ignored) {
        Menu drawerMenu = binding.navView.getMenu();
        drawerMenu.setGroupEnabled(R.id.langs_menu,true);
        drawerMenu.setGroupCheckable(R.id.langs_menu, true, true);
        drawerMenu.findItem(path.hashCode()).setChecked(true).setEnabled(false);
        Toast.makeText(this, "Loading "+name+"...", Toast.LENGTH_SHORT).show();
        ConWord.destroyEngine();
        ConWord.lang = path;
        binding.contentMain.toolbar.setTitle(name);
        ConWord.alphabet = alphabet;
        ConWord.ignored = ignored;
        ConWord.clongTypeface = clongUiTypeface(MainActivity.this);
        // TODO reload fragment
        this.getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragment_container_view_tag, BrowseFragment.class,null)
                .addToBackStack("dict").commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!super.onPrepareOptionsMenu(menu)) return false;
        for (Fragment f : this.getSupportFragmentManager().getFragments()) {
            if (f.isVisible()) {
                if (f instanceof BrowseFragment) {
                    menu.findItem(R.id.action_delete_entry).setVisible(false);
                    menu.findItem(R.id.action_settings).setVisible(true);
                }
                else if (f instanceof EntryFragment) {
                    menu.findItem(R.id.action_delete_entry).setVisible(true);
                    TextView tv = findViewById(R.id.word_display);
                    if (tv != null) menu.findItem(R.id.action_delete_entry).setTitle("Delete entry «"+tv.getText()+"»");
                    else menu.findItem(R.id.action_delete_entry).setTitle("Delete entry...");
                    menu.findItem(R.id.action_settings).setVisible(false);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
        case R.id.action_search:
            if (ConWord.lang == null) {
                Toast.makeText(this, "No language selected", Toast.LENGTH_SHORT).show();
                return false;
            }
            Intent i_s = new Intent(MainActivity.this, SearchActivity.class);
            i_s.setAction(Intent.ACTION_SEARCH);
            startActivity(i_s);
            return true;

        case R.id.action_settings:
            Intent sett = new Intent(MainActivity.this, LangSettingsActivity.class);
            startActivity(sett);
            return true;

        case R.id.action_login:
            mAuth.signOut();
            Intent lo = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(lo);
            return true;

        case R.id.action_upload:
            Intent sel = new Intent(Intent.ACTION_GET_CONTENT);
            sel.addCategory(Intent.CATEGORY_OPENABLE);
            sel.setType("*/*");
            Intent chooser = Intent.createChooser(sel, "Press start to introduce new project");
            startActivityForResult(chooser, 2);
            return true;

        case R.id.action_delete_entry:
            confirmDeleteCurrentEntry();
            break;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, (resultCode == RESULT_OK)?"все хорошо":"错了", Toast.LENGTH_SHORT);
        if (resultCode != RESULT_OK) return;
        Uri uri = data.getData();
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb0 = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                sb0.append(line);
            }
            String json = sb0.toString();
            JSONTokener jtk = new JSONTokener(json);
            JSONArray jArr = (JSONArray) jtk.nextValue();
            for (int i = 0; i < jArr.length(); i++) {
                JSONObject entry = jArr.getJSONObject(i);

                HashMap<String, Object> e_map = new HashMap<>();
                e_map.put("word",entry.getString("word").toString());
                e_map.put("pronunciation",entry.getString("pronunciation").toString());
                e_map.put("part_of_speech",entry.getString("part_speech")); // TODO
                e_map.put("definition",entry.getString("definition").toString());
                e_map.put("etymology",entry.getString("etymology").toString());
                db.collection(ConWord.lang) // TODO make variable
                        .add(e_map).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        System.out.println("congratusalafwihão");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.err.println("voi ei");
                    }
                });
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = null; //Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}