package edu.cmu.androidstuco.clongdict;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import edu.cmu.androidstuco.clongdict.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private HashMap<String,String> langs;
    public FirebaseFirestore db;

    // I may hold on to this for future use
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("RcvzM8NjXDiwqeO6PaegPzir994ema2tUlfQLKhY")
                .clientKey("KCTjOUxuvevuzZKHWlmCK5nMkevhlI0X6V2Gn3S9")
                .server("https://parseapi.back4app.com")
                .build()
        );//*/

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        /*
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
         */
        FragmentManager fm = this.getSupportFragmentManager();
        fm.beginTransaction().setReorderingAllowed(true)
                .replace(R.id.fragment_container_view_tag,FirstFragment.class,null)
                .addToBackStack("dict").commit();

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i0 = new Intent(MainActivity.this, EditActivity.class);
                if (findViewById(R.id.word_display)!=null) {
                    TextView tv = (TextView) findViewById(R.id.word_display);

                    // This grabs the ID of the given entry
                    // Distinct titles are assumed, but not currently enforced
                    db.collection("huoxinde-jazk") //TODO make var
                        .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            for (QueryDocumentSnapshot doc :
                                    task.getResult()) {
                                if (doc.getData().get("word").equals(tv.getText().toString())) {
                                    i0.putExtra("id", doc.getId());
                                    startActivity(i0);
                                }
                            }
                        }
                    });
                }
                else startActivity(i0);
            }
        });

        // Authorize firebase instance
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            // naturally this won't be hardcoded forever
            mAuth.signInWithEmailAndPassword("ccrawfor@andrew.cmu.edu","9shVB4JW")
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
        }

        // Language metadata is stored in firebase
        // not much is done with this now, but I plan to expand it so users can switch between languages
        db = FirebaseFirestore.getInstance();
        langs = new HashMap<>();
        db.collection("languages")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //Toast.makeText(MainActivity.this,document.getId() + " => " + document.getData(), Toast.LENGTH_LONG).show();
                                langs.put((String) document.getData().get("path"),(String) document.getData().get("Name"));
                            }
                            if (langs.containsKey("huoxinde-jazk")) binding.toolbar.setTitle(langs.get("huoxinde-jazk"));
                        } else {
                            System.err.println("Error getting documents.");
                            task.getException().printStackTrace();
                        }
                    }
                });
        /*
        HashMap<String, Object> testLang = new HashMap<>();
        testLang.put("Name", "Ol'ưnsih");
        testLang.put("path","olunsih");
        db.collection("languages").add(testLang);
         */
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
                if (f instanceof FirstFragment) {
                    menu.findItem(R.id.action_delete_entry).setVisible(false);
                    menu.findItem(R.id.action_settings).setVisible(true);
                }
                else if (f instanceof SecondFragment) {
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
        if (id == R.id.action_search) {
            Intent i_s = new Intent(MainActivity.this, SearchActivity.class);
            i_s.setAction(Intent.ACTION_SEARCH);
            startActivity(i_s);
            return true;
        }
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_upload) {
            Intent sel = new Intent(Intent.ACTION_GET_CONTENT);
            sel.addCategory(Intent.CATEGORY_OPENABLE);
            sel.setType("*/*");
            Intent i_s = Intent.createChooser(sel, "Press start to introduce new project");
            startActivityForResult(i_s, 2);
            return true;
        }
        if (id == R.id.action_delete_entry) {
            TextView tv = findViewById(R.id.word_display);
            if (tv == null) return false;
            db.collection("huoxinde-jazk") //TODO make var
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    for (QueryDocumentSnapshot doc :
                            task.getResult()) {
                        if (doc.getData().get("word").equals(tv.getText().toString())) {
                            Snackbar.make(MainActivity.this.findViewById(R.id.fragment_container_view_tag),
                                    "Deletion is irreversable, do you want to delete «"+doc.getData().get("word")+"»?",
                                    Snackbar.LENGTH_LONG)
                                    .setAction("CONFIRM", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // This actually deletes the thing
                                            db.collection("huoxinde-jazk") // TODO var
                                                    .document(doc.getId()).delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            getSupportFragmentManager().beginTransaction()
                                                                    .replace(R.id.fragment_container_view_tag,
                                                                            FirstFragment.class,
                                                                            null)
                                                                    .commit();
                                                            Snackbar.make(MainActivity.this
                                                                            .findViewById(R.id.fragment_container_view_tag),
                                                                    "Deletion Successful.", Snackbar.LENGTH_SHORT)
                                                                    .show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Snackbar.make(MainActivity.this
                                                                            .findViewById(R.id.fragment_container_view_tag),
                                                                    "Deletion Failed.", Snackbar.LENGTH_SHORT)
                                                                    .show();
                                                        }
                                                    });
                                        }
                                    })
                                    .setActionTextColor(0xc0ff0000)
                                    .show();
                        }
                    }
                }
            });
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, (resultCode == RESULT_OK)?"все хорошо":"错了", Toast.LENGTH_SHORT);
        if (resultCode != RESULT_OK) return;
        Uri uri = data.getData();
        File in = new File(uri.getPath());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(in));
            String json = "";
            /*
            String line;
            while ((line = reader.readLine()) != null) {
                json+=line;
            }
            Snackbar.make(binding.getRoot().getRootView(),json,Snackbar.LENGTH_LONG).show();
             */
            System.out.println("lolololololololololol\n\n\n\n\n\n\n\n\n\n\n\n\ntext\n\n\n\n\n\n\n");
        } catch (FileNotFoundException e) {
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