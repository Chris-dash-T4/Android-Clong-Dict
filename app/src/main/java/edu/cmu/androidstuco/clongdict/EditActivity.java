package edu.cmu.androidstuco.clongdict;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import edu.cmu.androidstuco.clongdict.databinding.ActivityEditBinding;

public class EditActivity extends AppCompatActivity {
    private ActivityEditBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_edit);

        Button save = (Button) findViewById(R.id.saveButton);
        if (save != null) {
            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                    Snackbar.make(view, "text:" + ((EditText)findViewById(R.id.word)).getText().toString(), Snackbar.LENGTH_LONG)
                            .setAction("a0", null).show();
                     */
                    Intent i1 = new Intent(EditActivity.this, MainActivity.class);
                    i1.putExtra("entry", ((EditText)findViewById(R.id.word)).getText().toString());
                    i1.putExtra("pronunciation", ((EditText)findViewById(R.id.pronunciation)).getText().toString());
                    i1.putExtra("def", ((EditText)findViewById(R.id.definition)).getText().toString());
                    i1.putExtra("etym", "placeholder");
                    startActivity(i1);
                }
            });
        }
    }
}