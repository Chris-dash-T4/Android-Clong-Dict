package edu.cmu.androidstuco.clongdict;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

import edu.cmu.androidstuco.clongdict.databinding.ActivityEditBinding;

public class EditActivity extends AppCompatActivity {
    private ActivityEditBinding binding;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        if (getIntent().getExtras() != null && getIntent().getExtras().get("id") != null) {
            String entry_id = (String) getIntent().getExtras().get("id");
            if (ConWord.lang == null) {
                Toast.makeText(this, "No language selected", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection(ConWord.lang).document(entry_id).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                    Toast.makeText(EditActivity.this, "Could not load entry.", Toast.LENGTH_SHORT).show();
                    return;
                }
                DocumentSnapshot doc = task.getResult();
                Object w = doc.get("word");
                if (w != null) binding.word.setText(w.toString());
                Object pron = doc.get("pronunciation");
                if (pron != null) binding.pronunciation.setText(pron.toString());
                Object pos = doc.get("part_of_speech");
                if (pos == null) pos = doc.get("lex_category");
                if (pos != null) binding.partOfSpeech.setText(pos.toString());
                Object def = doc.get("definition");
                if (def != null) binding.definition.setText(def.toString());
                Object etym = doc.get("etymology");
                if (etym != null) binding.etymology.setText(etym.toString());
            });
        }

        binding.saveButton.bringToFront();
    }

    private void performFirestoreAdd(@NonNull HashMap<String, Object> e_map) {
        Snackbar s0 = Snackbar.make(binding.getRoot(), "Saving...", Snackbar.LENGTH_INDEFINITE);
        s0.show();
        db.collection(ConWord.lang).add(e_map).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        s0.dismiss();
                        startActivity(new Intent(EditActivity.this, MainActivity.class));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        s0.dismiss();
                        Snackbar.make(binding.getRoot(), "Oops! Something went wrong.", Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    public void saveBtnClicked(View view) {
        Intent i1 = new Intent(EditActivity.this, MainActivity.class);
        HashMap<String, Object> e_map = new HashMap<>();
        e_map.put("word", binding.word.getText().toString());
        e_map.put("pronunciation", binding.pronunciation.getText().toString());
        e_map.put("part_of_speech", binding.partOfSpeech.getText().toString());
        e_map.put("definition", binding.definition.getText().toString());
        e_map.put("etymology", binding.etymology.getText().toString());
        Snackbar s0 = Snackbar.make(binding.getRoot(), "Saving...", Snackbar.LENGTH_INDEFINITE);
        if (getIntent().getExtras() != null && getIntent().getExtras().get("id") != null) {
            String entry_id = (String) getIntent().getExtras().get("id");
            if (ConWord.lang == null) {
                Toast.makeText(this, "No language selected", Toast.LENGTH_SHORT).show();
                return;
            }
            s0.show();
            db.collection(ConWord.lang).document(entry_id).set(e_map)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unit) {
                            s0.dismiss();
                            startActivity(i1);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            s0.dismiss();
                            Snackbar.make(binding.getRoot(), "Oops! Something went wrong.", Snackbar.LENGTH_SHORT).show();
                        }
                    });
        } else {
            if (ConWord.lang == null) {
                Toast.makeText(this, "No language selected", Toast.LENGTH_SHORT).show();
                return;
            }
            String lemma = binding.word.getText().toString().trim();
            if (lemma.isEmpty()) {
                performFirestoreAdd(e_map);
                return;
            }
            db.collection(ConWord.lang).whereEqualTo("word", lemma).get()
                    .addOnCompleteListener((Task<QuerySnapshot> task) -> {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            Toast.makeText(EditActivity.this,
                                    "Could not check for duplicates.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        QuerySnapshot qs = task.getResult();
                        if (!qs.isEmpty()) {
                            new AlertDialog.Builder(EditActivity.this)
                                    .setTitle("Possible duplicate")
                                    .setMessage("Another entry already uses this lemma. Save anyway?")
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton("Save anyway", (dialog, which) -> performFirestoreAdd(e_map))
                                    .show();
                        } else {
                            performFirestoreAdd(e_map);
                        }
                    });
        }
    }
}
