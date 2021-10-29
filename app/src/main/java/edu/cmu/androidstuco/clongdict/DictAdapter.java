package edu.cmu.androidstuco.clongdict;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NOTE: most of this code was unceremoniously yoinked from the Android GitHub page
 */
public class DictAdapter extends RecyclerView.Adapter<DictAdapter.ViewHolder> {
    private static final String TAG = "DictAdapter";

    private ArrayList<DictEntry> mDataSet;

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView wordView;
        private final TextView defView;
        private final TextView pronView;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
                    //System.out.println(getAdapterPosition() + " clicked");

                    // thank you stackoverflow
                    Context c0 = v.getContext();
                    MainActivity a = null;
                    while (c0 instanceof ContextWrapper && a==null) {
                        if (c0 instanceof MainActivity) a = (MainActivity) c0;
                        else c0 = ((ContextWrapper)c0).getBaseContext();
                    }
                    if (a==null) Snackbar.make(v,"thing is not", Snackbar.LENGTH_SHORT).show();
                    else Toast.makeText(a,getAdapterPosition()+" clicked", Toast.LENGTH_LONG);
                    FragmentManager fs = a.getSupportFragmentManager();//.getFragments();
                    // TODO get entryFragment textviews
                    //fs.findFragmentById(R.id.entryFragment).getView();
                    NavHostFragment.findNavController(fs.findFragmentById(R.id.dictFragment))
                            .navigate(R.id.action_FirstFragment_to_SecondFragment);
                    /*
                     */
                }
            });
            wordView = (TextView) v.findViewById(R.id.dictWordTV);
            defView = (TextView) v.findViewById(R.id.dictDefTV);
            pronView = (TextView) v.findViewById(R.id.dictPronTV);
        }

        public TextView getWordView() {
            return wordView;
        }

        public TextView getDefView() {
            return defView;
        }

        public TextView getPronView() {
            return pronView;
        }
    }
    // END_INCLUDE(recyclerViewSampleViewHolder)

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet DictEntry[] containing the data to populate views to be used by RecyclerView.
     */
    public DictAdapter(DictEntry[] dataSet) {
        mDataSet = new ArrayList<>(Arrays.asList(dataSet));
        mDataSet.add(0,new DictEntry("ArrayList Moment","sæm.pl", DictEntry.PartOfSpeech.UNDEFINED,"Западный текст",""));
    }

    // BEGIN_INCLUDE(recyclerViewOnCreateViewHolder)
    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.dict_entry, viewGroup, false);

        return new ViewHolder(v);
    }
    // END_INCLUDE(recyclerViewOnCreateViewHolder)

    // BEGIN_INCLUDE(recyclerViewOnBindViewHolder)
    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");
        System.out.println("em: "+position+" set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        viewHolder.getWordView().setText(mDataSet.get(position).getWord());
        viewHolder.getPronView().setText(mDataSet.get(position).getPronunciation());
        viewHolder.getDefView().setText(mDataSet.get(position).getDefinition());
    }
    // END_INCLUDE(recyclerViewOnBindViewHolder)

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void pushElement(DictEntry e) {
        this.mDataSet.add(0,e);
        return;
    }
}
