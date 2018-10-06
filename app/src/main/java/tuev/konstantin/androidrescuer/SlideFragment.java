package tuev.konstantin.androidrescuer;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.content.ContentValues.TAG;

public class SlideFragment extends agency.tango.materialintroscreen.SlideFragment {
    private final static String BACKGROUND_COLOR = "background_color";
    private static final String BUTTONS_COLOR = "buttons_color";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String NEEDED_PERMISSIONS = "needed_permission";
    private static final String POSSIBLE_PERMISSIONS = "possible_permission";
    private static final String IMAGE = "image";
    private static final int PERMISSIONS_REQUEST_CODE = 15621;
    private static final String IMAGE_URL = "image_url";

    private int backgroundColor;
    private int buttonsColor;
    private int image;
    private String title;
    private ArrayList<String> description;
    private String[] neededPermissions;
    private String[] possiblePermissions;

    private TextView titleTextView;
    private TextView descriptionTextView;
    private ImageView imageView;
    private String imageUrl;

    public static SlideFragment createInstance(SlideFragmentBuilder builder) {
        SlideFragment slideFragment = new SlideFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(BACKGROUND_COLOR, builder.backgroundColor);
        bundle.putInt(BUTTONS_COLOR, builder.buttonsColor);
        bundle.putInt(IMAGE, builder.image);
        bundle.putString(TITLE, builder.title);
        bundle.putStringArrayList(DESCRIPTION, builder.description);
        bundle.putStringArray(NEEDED_PERMISSIONS, builder.neededPermissions);
        bundle.putStringArray(POSSIBLE_PERMISSIONS, builder.possiblePermissions);
        bundle.putString(IMAGE_URL, builder.imageUrl);

        slideFragment.setArguments(bundle);
        return slideFragment;
    }

    public static boolean isNotNullOrEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slide, container, false);
        titleTextView = view.findViewById(R.id.txt_title_slide);
        descriptionTextView = view.findViewById(R.id.txt_description_slide);
        imageView = view.findViewById(R.id.image_slide);
        initializeView();
        return view;
    }

    public void initializeView() {
        Bundle bundle = getArguments();
        backgroundColor = bundle.getInt(BACKGROUND_COLOR);
        buttonsColor = bundle.getInt(BUTTONS_COLOR);
        image = bundle.getInt(IMAGE, android.R.drawable.ic_notification_clear_all);
        title = bundle.getString(TITLE);
        description = bundle.getStringArrayList(DESCRIPTION);
        neededPermissions = bundle.getStringArray(NEEDED_PERMISSIONS);
        possiblePermissions = bundle.getStringArray(POSSIBLE_PERMISSIONS);
        imageUrl = bundle.getString(IMAGE_URL);

        updateViewWithValues();
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public int buttonsColor() {
        return buttonsColor;
    }

    public boolean hasAnyPermissionsToGrant() {
        boolean hasPermissionToGrant = hasPermissionsToGrant(neededPermissions);
        if (!hasPermissionToGrant) {
            hasPermissionToGrant = hasPermissionsToGrant(possiblePermissions);
        }
        return hasPermissionToGrant;
    }

    public boolean hasNeededPermissionsToGrant() {
        return hasPermissionsToGrant(neededPermissions);
    }

    public boolean canMoveFurther() {
        return true;
    }

    public String cantMoveFurtherErrorMessage() {
        return getString(R.string.impassable_slide);
    }

    private void updateViewWithValues() {
        titleTextView.setText(title);
        descriptionTextView.setMovementMethod(LinkMovementMethod.getInstance());
        descriptionTextView.setLinkTextColor(getResources().getColor(android.R.color.white));
        SpannableStringBuilder descBuilder = new SpannableStringBuilder();

        if (description.size() > 1) {
            for (int i = 0; i < description.size(); i++) {
                String item = description.get(i);

                if (item.length() > 61) {
                    SpannableString ss = new SpannableString(item.substring(0, 61) + "..." + getString(R.string.read_more));
                    String finalItem = item.substring(1);
                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void updateDrawState(TextPaint ds) {
                            ds.setUnderlineText(false);
                        }

                        @Override
                        public void onClick(View textView) {
                            new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                                    .setTitle(R.string.more_info)
                                    .setMessage(finalItem)
                                    .setPositiveButton("OK", (dialogInterface, i) -> {}).show();
                        }
                    };
                    ss.setSpan(clickableSpan, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.darker_gray)), 64, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    descBuilder.append(ss);
                } else {
                    descBuilder.append(item);
                }
                if ((i + 1) != description.size()) {
                    descBuilder.append("\n\n");
                }
            }
        } else {
            descBuilder.append(description.get(0));
        }
        descriptionTextView.setText(descBuilder);

        if (image != 0) {
            imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), image));
            imageView.setVisibility(View.VISIBLE);
        }
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageView.setVisibility(View.VISIBLE);
            //Log.d(TAG, "updateViewWithValues: "+Helper.getServerImageUrl(getContext(), imageUrl));
            Glide.with(this).load(Helper.getServerImageUrl(getContext(), imageUrl)).into(imageView);
        }
    }

    public void askForPermissions() {
        ArrayList<String> notGrantedPermissions = new ArrayList<>();

        if (neededPermissions != null) {
            for (String permission : neededPermissions) {
                if (isNotNullOrEmpty(permission)) {
                    if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                        notGrantedPermissions.add(permission);
                    }
                }
            }
        }
        if (possiblePermissions != null) {
            for (String permission : possiblePermissions) {
                if (isNotNullOrEmpty(permission)) {
                    if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                        notGrantedPermissions.add(permission);
                    }
                }
            }
        }

        String[] permissionsToGrant = removeEmptyAndNullStrings(notGrantedPermissions);
        ActivityCompat.requestPermissions(getActivity(), permissionsToGrant, PERMISSIONS_REQUEST_CODE);
    }

    private boolean hasPermissionsToGrant(String[] permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (isNotNullOrEmpty(permission)) {
                    if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private String[] removeEmptyAndNullStrings(final ArrayList<String> permissions) {
        List<String> list = new ArrayList<>(permissions);
        list.removeAll(Collections.singleton(null));
        return list.toArray(new String[list.size()]);
    }
}
