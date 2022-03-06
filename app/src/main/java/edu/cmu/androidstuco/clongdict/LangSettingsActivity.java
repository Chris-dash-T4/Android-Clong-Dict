package edu.cmu.androidstuco.clongdict;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

public class LangSettingsActivity extends AppCompatActivity {
    private static String langId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem saveBtn = menu.add("SAVE").setIcon(android.R.drawable.ic_menu_save);
        saveBtn.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        saveBtn.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Toast.makeText(LangSettingsActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
                SettingsFragment sf = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings);
                if (sf == null) return false;
                HashMap<String, Object> l_map = new HashMap<>();
                l_map.put("Name",((EditTextPreference) sf.findPreference("name")).getText().toString());
                l_map.put("path",((EditTextPreference) sf.findPreference("path")).getText().toString());
                l_map.put("alphabet",((EditTextPreference) sf.findPreference("alphabet")).getText().toString());
                l_map.put("ignored",((EditTextPreference) sf.findPreference("ignored")).getText().toString());
                FirebaseFirestore.getInstance().collection("languages").document(langId)
                        .set(l_map).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if (!ConWord.lang.equals(l_map.get("path"))) ConWord.lang=(String) l_map.get("path");
                        if (!ConWord.alphabet.equals(l_map.get("alphabet")) ||
                            !ConWord.ignored.equals(l_map.get("ignored"))) {
                            ConWord.alphabet = (String) l_map.get("alphabet");
                            ConWord.ignored = (String) l_map.get("ignored");
                            DictAdapter.resetAlph = true;
                        }
                        Intent i0 = new Intent(LangSettingsActivity.this, MainActivity.class);
                        startActivity(i0);
                    }
                });
                return false;
            }
        });
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("languages").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    for (QueryDocumentSnapshot document :
                            task.getResult()) {
                        langId = document.getId();
                        if (ConWord.lang.equals((String) document.getData().get("path"))) {
                            ((EditTextPreference) findPreference("path")).setText(ConWord.lang);
                            ((EditTextPreference) findPreference("name")).setText((String) document.getData().get("Name"));
                            ((EditTextPreference) findPreference("alphabet")).setText((String) document.getData().get("alphabet"));
                            ((EditTextPreference) findPreference("ignored")).setText((String) document.getData().get("ignored"));
                        }
                    }
                }
            });
        }
    }
}