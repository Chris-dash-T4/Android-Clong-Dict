package edu.cmu.androidstuco.clongdict;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
        setContentView(R.layout.activity_edit);

        db = FirebaseFirestore.getInstance();
        if (getIntent().getExtras()!=null && getIntent().getExtras().get("id")!=null) {
            // If the activity was accessed to edit an existing entry, fill the fields with
            // the existing data for that entry
            String entry_id = (String) getIntent().getExtras().get("id");
            db.collection(ConWord.lang) // TODO var
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    for (QueryDocumentSnapshot doc :
                            task.getResult()) {
                        if (doc.getId().equals(entry_id)) {
                            //Toast.makeText(EditActivity.this, (String) doc.getData().get("word"), Toast.LENGTH_SHORT).show();
                            ((TextView) findViewById(R.id.word)).setText((CharSequence) doc.getData().get("word"), TextView.BufferType.EDITABLE);
                            ((TextView) findViewById(R.id.pronunciation)).setText((CharSequence) doc.getData().get("pronunciation"), TextView.BufferType.EDITABLE);
                            ((TextView) findViewById(R.id.partOfSpeech)).setText((CharSequence) doc.getData().get("part_of_speech"), TextView.BufferType.EDITABLE);
                            ((TextView) findViewById(R.id.definition)).setText((CharSequence) doc.getData().get("definition"), TextView.BufferType.EDITABLE);
                            ((TextView) findViewById(R.id.etymology)).setText((CharSequence) doc.getData().get("etymology"), TextView.BufferType.EDITABLE);
                        }
                    }
                }
            });
        }

        binding.saveButton.bringToFront();
    }

    public void saveBtnClicked(View view) {
        Intent i1 = new Intent(EditActivity.this, MainActivity.class);
        HashMap<String, Object> e_map = new HashMap<>();
        e_map.put("word",((EditText)findViewById(R.id.word)).getText().toString());
        e_map.put("pronunciation",((EditText)findViewById(R.id.pronunciation)).getText().toString());
        e_map.put("part_of_speech",((EditText)findViewById(R.id.partOfSpeech)).getText().toString());
        e_map.put("definition",((EditText)findViewById(R.id.definition)).getText().toString());
        e_map.put("etymology",((EditText)findViewById(R.id.etymology)).getText().toString());
        Snackbar s0 = Snackbar.make(view.getRootView(),"Saving...", Snackbar.LENGTH_INDEFINITE);
        if (getIntent().getExtras()!=null && getIntent().getExtras().get("id")!=null) {
            String entry_id = (String) getIntent().getExtras().get("id");
            db.collection(ConWord.lang) // TODO make variable
                    .document(entry_id).set(e_map)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unit) {
                            s0.dismiss();
                            // Don't return to the main activity until all data has been successfully saved
                            startActivity(i1);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            s0.dismiss();
                            Snackbar.make(view.getRootView(), "Oops! Something went wrong.", Snackbar.LENGTH_SHORT).show();
                        }
                    });
        }
        else {
            db.collection(ConWord.lang) // TODO make variable
                    .add(e_map).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            s0.dismiss();
                            startActivity(i1);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            s0.dismiss();
                            Snackbar.make(view.getRootView(), "Oops! Something went wrong.", Snackbar.LENGTH_SHORT).show();
                        }
                    });
        }
        s0.show();
    }
}