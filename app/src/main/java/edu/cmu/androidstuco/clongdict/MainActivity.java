package edu.cmu.androidstuco.clongdict;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.RecyclerView;

import edu.cmu.androidstuco.clongdict.databinding.ActivityMainBinding;
import edu.cmu.androidstuco.clongdict.EditActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

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
                startActivity(i0);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = null; //Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void setSndArgs(Bundle args) {
        Fragment snd = this.getSupportFragmentManager().findFragmentById(R.id.entryFragment);
        snd.setArguments(args);
    }
}