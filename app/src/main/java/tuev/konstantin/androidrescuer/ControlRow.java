package tuev.konstantin.androidrescuer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;

public class ControlRow extends RelativeLayout {
    private AppCompatSpinner spinner;
    private static SharedPreferences prefs;

    public static SharedPreferences getPrefs(Context context) {
        if (prefs == null) {
            return (prefs = context.getSharedPreferences("ControlRow", Context.MODE_PRIVATE));
        } else {
            return prefs;
        }
    }

    public ControlRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.row_control, this, true);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ControlRow,
                0, 0);

        try {
            String text = a.getString(R.styleable.ControlRow_text);
            final String idString = a.getString(R.styleable.ControlRow_id);
            boolean twoOptions = a.getBoolean(R.styleable.ControlRow_twoOptions, false);
            ((TextView)findViewById(R.id.action)).setText(text);
            spinner = findViewById(R.id.options);
            ArrayList<String> items = new ArrayList<>();
            if (!twoOptions) {
                items.add(getContext().getText(R.string.def).toString());
                items.add(getContext().getText(R.string.enable).toString());
                items.add(getContext().getText(R.string.disable).toString());
            } else {
                items.add(getContext().getText(R.string.no).toString());
                items.add(getContext().getText(R.string.yes).toString());
            }
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getContext(), R.layout.simple_dropdown_black_black, items);
            spinner.setAdapter(spinnerArrayAdapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    getPrefs(getContext()).edit().putInt(idString, position).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            spinner.setSelection(getPrefs(getContext()).getInt(idString, 0), true);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            a.recycle();
        }
    }
}
