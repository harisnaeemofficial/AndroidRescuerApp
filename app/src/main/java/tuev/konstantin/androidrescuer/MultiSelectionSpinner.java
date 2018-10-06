package tuev.konstantin.androidrescuer;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.TintTypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class MultiSelectionSpinner extends AppCompatSpinner {
    CharSequence[] _items = null;
    boolean[] mSelection = null;

    ArrayAdapter<CharSequence> simple_adapter;

    AlertDialog dialog = null;
    private ListView lv = null;
    private boolean custom = false;
    private String id = null;

    public MultiSelectionSpinner(Context context) {
        super(context);

        simple_adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item);
        super.setAdapter(simple_adapter);
    }

    public MultiSelectionSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.MultiSelectionSpinner);

        _items = a.getTextArray(R.styleable.MultiSelectionSpinner_checkEntries);
        custom = a.getBoolean(R.styleable.MultiSelectionSpinner_custom, false);
        id = a.getString(R.styleable.MultiSelectionSpinner_idSpin);

        a.recycle();
        if (custom) {
            simple_adapter = new ArrayAdapter<>(context,
                    R.layout.spinner_item, new CharSequence[]{_items[0], getContext().getString(R.string.take_actions)});
            try {
                mSelection = new Gson().fromJson(Helper.sharedPrefs(getContext()).getString(id, null), boolean[].class);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            super.setAdapter(simple_adapter);
            if (mSelection == null) {
                if (_items != null) {
                    mSelection = new boolean[_items.length];
                } else {
                    mSelection = new boolean[0];
                }
                mSelection[0] = true;
            } else {
                setSelection(mSelection[0] ? 0 : 1);
            }
        } else {
            AlarmManager alarm = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            simple_adapter = new ArrayAdapter<>(context,
                    R.layout.simple_dropdown_black_black, _items);
            setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Helper.sharedPrefs(getContext()).edit().putInt(MultiSelectionSpinner.this.id, position).apply();
                    int timeToRefresh = new int[]{0, 3, 6, 9, 12}[position];
                    Log.d("TAG", "onItemSelected: pos: "+position+" time: "+timeToRefresh);
                    Intent startService = new Intent(context, ProtectorService.class);
                    startService.putExtra("action", "sendLocation");
                    PendingIntent pintent;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        pintent = PendingIntent
                                .getForegroundService(getContext(), 6531, startService, PendingIntent.FLAG_UPDATE_CURRENT);

                    } else {
                        pintent = PendingIntent
                                .getService(getContext(), 6531, startService, PendingIntent.FLAG_UPDATE_CURRENT);

                    }
                    if (timeToRefresh > 0) {
                        Calendar cal = Calendar.getInstance();

                        if (alarm != null) {
                            alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                                    timeToRefresh*3600*1000, pintent);
                        }
                    } else {
                        if (alarm != null) {
                            alarm.cancel(pintent);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            super.setAdapter(simple_adapter);
            setSelection(Helper.sharedPrefs(getContext()).getInt(MultiSelectionSpinner.this.id, 0));
        }
    }

    public void onClick(int which, boolean isChecked) {
        if (mSelection != null && which < mSelection.length) {
            mSelection[which] = isChecked;

            boolean hasTrue = false;
            for (int i = 1; i < mSelection.length; i++) {
                if (mSelection[i]) {
                    hasTrue = true;
                    break;
                }
            }
            if (hasTrue) {
                if (which == 0) {
                    setSelection(0);
                    mSelection = new boolean[_items.length];
                    mSelection[0] = true;

                    lv.setItemChecked(0, true);
                    lv.setItemChecked(1, false);
                    lv.setItemChecked(2, false);
                    lv.setItemChecked(3, false);
                    lv.setItemChecked(4, false);
                } else if (which == 3 || which == 4) {
                    setSelection(1);
                    mSelection[0] = false;
                    mSelection[1] = false;
                    mSelection[2] = false;

                    lv.setItemChecked(0, false);
                    lv.setItemChecked(1, false);
                    lv.setItemChecked(2, false);
                } else if (which == 1 || which == 2) {
                    setSelection(1);
                    mSelection[0] = false;
                    mSelection[3] = false;
                    mSelection[4] = false;

                    lv.setItemChecked(0, false);
                    lv.setItemChecked(3, false);
                    lv.setItemChecked(4, false);
                }
            } else {
                setSelection(0);
                mSelection[0] = true;

                lv.setItemChecked(0, true);
            }
        }
    }

    @Override
    public boolean performClick() {
        if (custom) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setOnCancelListener(dialog -> {
                Helper.sharedPrefs(getContext()).edit().putString(id, new Gson().toJson(mSelection)).apply();
                boolean hasTrue = false;
                for (int i = 1; i < mSelection.length; i++) {
                    if (mSelection[i]) {
                        hasTrue = true;
                        break;
                    }
                }
                if (hasTrue) {
                    Intent startService = new Intent(getContext(), ProtectorService.class);
                    startService.putExtra("action", "simListener");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getContext().startForegroundService(startService);
                    } else {
                        getContext().startService(startService);
                    }
                }
            });
            lv = new ListView(getContext());
            lv.setAdapter(new ArrayAdapter<CharSequence>(
                    getContext(), R.layout.checked_layout, android.R.id.text1, _items) {
                @NonNull
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    if (mSelection != null) {
                        boolean isItemChecked = mSelection[position];
                        if (isItemChecked) {
                            lv.setItemChecked(position, true);
                        }
                    }
                    return view;
                }
            });
            lv.setOnItemClickListener((parent, view, position, id) -> onClick(position, lv.isItemChecked(position)));
            lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            dialog = builder.show();
            dialog.setContentView(lv);
            return true;
        } else {
            super.performClick();
        }
        return true;
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {
        if (!custom) {
            super.setAdapter(adapter);
        }
    }

}
