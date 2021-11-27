package edu.cmu.androidstuco.clongdict;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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

        binding.saveButton.bringToFront();
    }

    public void saveBtnClicked(View view) {
        Intent i1 = new Intent(EditActivity.this, MainActivity.class);
        HashMap<String, Object> e_map = new HashMap<>();
        e_map.put("word",((EditText)findViewById(R.id.word)).getText().toString());
        e_map.put("pronunciation",((EditText)findViewById(R.id.pronunciation)).getText().toString());
        e_map.put("part_of_speech","Undefined"); // TODO
        e_map.put("definition",((EditText)findViewById(R.id.definition)).getText().toString());
        e_map.put("etymology",((EditText)findViewById(R.id.etymology)).getText().toString());
        Snackbar s0 = Snackbar.make(view.getRootView(),"Saving...", Snackbar.LENGTH_INDEFINITE);
        db.collection("huoxinde-jazk") // TODO make variable
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
                Snackbar.make(view.getRootView(),"Oops! Something went wrong.", Snackbar.LENGTH_SHORT).show();
            }
        });
        s0.show();
    }
}