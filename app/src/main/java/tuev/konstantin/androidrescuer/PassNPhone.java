package tuev.konstantin.androidrescuer;

import java.util.ArrayList;

public class PassNPhone extends ArrayList<String>{
    public String getPass() {
        return get(1);
    }
    public String getPhone() {
        return get(0);
    }
    public PassNPhone(String pass, String phone) {
        if (size() == 1) {
            set(0, phone);
        } else if(size() >= 2) {
            set(0, phone);
            set(1, pass);
        } else  {
            add(phone);
            add(pass);
        }
    }
}
