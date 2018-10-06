package tuev.konstantin.androidrescuer;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;

import java.util.ArrayList;

public class SlideFragmentBuilder {
    int backgroundColor;
    int buttonsColor;
    String title;
    ArrayList<String> description = new ArrayList<>();
    String[] neededPermissions;
    String[] possiblePermissions;
    int image;
    String imageUrl;

    public SlideFragmentBuilder backgroundColor(@ColorRes int backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public SlideFragmentBuilder buttonsColor(@ColorRes int buttonsColor) {
        this.buttonsColor = buttonsColor;
        return this;
    }

    public SlideFragmentBuilder title(String title) {
        this.title = title;
        return this;
    }

    public SlideFragmentBuilder description(String description) {
        this.description.add(description);
        return this;
    }

    public SlideFragmentBuilder neededPermissions(String[] neededPermissions) {
        this.neededPermissions = neededPermissions;
        return this;
    }

    public SlideFragmentBuilder possiblePermissions(String[] possiblePermissions) {
        this.possiblePermissions = possiblePermissions;
        return this;
    }

    private SlideFragmentBuilder image(@DrawableRes int image) {
        this.image = image;
        return this;
    }

    public SlideFragment build() {
        return SlideFragment.createInstance(this);
    }

    public SlideFragmentBuilder image(String serverImage) {
        this.imageUrl = serverImage;
        return this;
    }
}
