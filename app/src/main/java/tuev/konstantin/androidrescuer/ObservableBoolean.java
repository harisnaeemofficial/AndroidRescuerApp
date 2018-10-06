package tuev.konstantin.androidrescuer;

import java.util.Observable;
import java.util.Observer;

class ObservableBoolean extends Observable {
    private boolean value = false;

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        this.setChanged();
        this.notifyObservers(value);
    }
}
