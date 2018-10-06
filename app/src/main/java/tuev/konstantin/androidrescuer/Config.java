package tuev.konstantin.androidrescuer;

class Config {
    static final String REGISTRATION_COMPLETE = "registrationComplete";
    static final String PUSH_NOTIFICATION = "pushNotification";

    static final String MAIL = "mail";
    static final String FCM_TOKEN = "fcm_token";
    static final String APP_PASS_CONTAIN = "pass";
    static final String PHONE_CONTAIN = "phone";

    public enum ResponseJson {
        TEXT("result"),
        ERROR("error");

        private String value;
        ResponseJson(String item) {
            value = item;
        }

         @Override
         public String toString() {
             return value;
         }
    }

    static final boolean test = true;


    public enum controlService{
        START,
        STOP,
        FREE_WIFI_SCAN,
        DISABLE_LOCK_POWERMENU,
        LISTEN_FOR_SIM_CHANGES,
        LOCATION_UPDATES

    }
}
