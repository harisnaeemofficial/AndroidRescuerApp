package tuev.konstantin.androidrescuer;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static android.app.admin.DevicePolicyManager.WIPE_EXTERNAL_STORAGE;
import static tuev.konstantin.androidrescuer.MainActivity.TAG;

@SuppressWarnings({"SameParameterValue", "unused"})
interface IHandleControlMessage {
    void prepareContacts(MediaPlayer.OnPreparedListener preparedListener);

    void backupContacts();
    void backupRescueFolder();
    void locationOnWrongPass(boolean state);
    void pictureOnWrongPass(boolean state);
    void locationTracking(boolean state);
    void mobileData(boolean state);
    void locationGPS(boolean state);
    void factoryDataReset(boolean deleteInternal);
    void deleteInternalStorage();
    void handleNetLost(boolean state);

    void startLogging();

    void sendCollectedData(boolean contacts);
}

@SuppressWarnings({"ResultOfMethodCallIgnored", "JavaDoc"})
public class HandleControlMessage implements IHandleControlMessage {

    private GoogleApiClient mGoogleApiClient;
    public Context context;
    private boolean connected = false;
    private DevicePolicyManager devicePolicyManager;
    private String contactID;
    private Uri uriContact;

    HandleControlMessage(Context context, boolean googleAPI) {
        this.context = context;
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName demoDeviceAdmin = new ComponentName(context, Admin.class);
        if (devicePolicyManager != null && !devicePolicyManager.isAdminActive(demoDeviceAdmin)) {
            devicePolicyManager = null;
        }
        if (googleAPI) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {
                            connected = true;
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            connected = false;
                        }
                    })
                    .addOnConnectionFailedListener(connectionResult -> connected = false)
                    .build();
        }
    }

    private String vcfContents = null;

    @Override
    public void prepareContacts(MediaPlayer.OnPreparedListener preparedListener) {
        new Thread(() -> {
            vcfContents = getVCF();
            File sourceFile = new File(Environment.getExternalStorageDirectory()+"/rescue");
            if (!sourceFile.exists()) {
                sourceFile = new File(Environment.getExternalStorageDirectory()+"/Rescue");
                if (!sourceFile.exists()) {
                    sourceFile.mkdirs();
                }
            }
            sourceFile = new File(sourceFile+"/data");
            sourceFile.mkdirs();
            File contacts = new File(sourceFile + "/contacts.vcf");
            if (!contacts.exists()) {
                try {
                    contacts.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                OutputStream stream = new BufferedOutputStream(new FileOutputStream(contacts));
                stream.write(vcfContents.getBytes());
                stream.flush();
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            preparedListener.onPrepared(null);
        }).run();
    }

    @Override
    public void backupContacts() {
        Log.d(TAG, "backupContacts");
        if (mGoogleApiClient == null) {
            Log.d(TAG, "backupContacts: mGoogleApiClient: null");
            return;
        }

        if (!connected) {
            mGoogleApiClient.connect();
        }
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(result -> {
                    if (!result.getStatus().isSuccess()) {
                        return;
                    }
                    final DriveContents driveContents = result.getDriveContents();

                    // Perform I/O off the UI thread.
                    Thread writeContacts = new Thread() {
                        @Override
                        public void run() {
                            if (vcfContents == null) {
                                vcfContents = getVCF();
                            }
                            if (vcfContents.isEmpty()) {
                                return;
                            }
                            // write content to DriveContents
                            OutputStream outputStream = driveContents.getOutputStream();
                            Writer writer = new OutputStreamWriter(outputStream);
                            try {
                                writer.write(vcfContents);
                                writer.close();
                            } catch (IOException e) {
                                Log.e(TAG, e.getMessage());
                            }

                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle("Contacts from "+android.text.format.DateFormat.format("dd-MM-yyyy", new Date())+".vcf")
                                    .setMimeType("text/vcard")
                                    .setStarred(true).build();

                            // create a file on root folder
                            Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                    .createFile(mGoogleApiClient, changeSet, driveContents)
                                    .setResultCallback(driveFileResult -> {
                                        if (!driveFileResult.getStatus().isSuccess()) {
                                            Handler mainHandler = new Handler(context.getMainLooper());

                                            Runnable myRunnable = () -> backupContacts();
                                            mainHandler.post(myRunnable);
                                        }
                                    });
                        }
                    };
                    writeContacts.start();
                });
    }

    @Override
    public void backupRescueFolder() {
        Log.d(TAG, "backupRescueFolder");
        if (mGoogleApiClient == null) {
            Log.d(TAG, "backupRescueFolder: mGoogleApiClient: null");return;}
        if (!connected) {
            mGoogleApiClient.connect();
        }
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(result -> {
                    if (!result.getStatus().isSuccess()) {
                        return;
                    }

                    File sourceFile = new File(Environment.getExternalStorageDirectory()+"/Rescue");
                    if (!sourceFile.exists()) {
                        sourceFile = new File(Environment.getExternalStorageDirectory()+"/rescue");
                        if (!sourceFile.exists()) {
                            return;
                        }
                    }

                    final DriveContents driveContents = result.getDriveContents();

                    // Perform I/O off the UI thread.
                    File finalSourceFile = sourceFile;
                    Log.d(TAG, "run: do stuff: "+finalSourceFile);
                    Thread writeContacts = new Thread() {
                        @Override
                        public void run() {
                            // write content to DriveContents
                            OutputStream outputStream = driveContents.getOutputStream();

                            try {
                                zipDirectory(finalSourceFile, outputStream);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle("Rescue folder from "+android.text.format.DateFormat.format("dd-MM-yyyy", new Date())+".zip")
                                    .setMimeType("application/zip")
                                    .setStarred(true).build();

                            // create a file on root folder
                            Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                    .createFile(mGoogleApiClient, changeSet, driveContents)
                                    .setResultCallback(driveFileResult -> {
                                        if (!driveFileResult.getStatus().isSuccess()) {
                                            Handler mainHandler = new Handler(context.getMainLooper());

                                            Runnable myRunnable = () -> backupRescueFolder();
                                            mainHandler.post(myRunnable);
                                        }
                                    });
                        }
                    };
                    writeContacts.start();
                });
    }

    @Override
    public void locationOnWrongPass(boolean state) {
        Log.d(TAG, "locationOnWrongPass: "+state);
        Helper.sharedPrefs(context).edit().putBoolean("wrongPassLocation", state).apply();
    }

    @Override
    public void pictureOnWrongPass(boolean state) {
        Log.d(TAG, "pictureOnWrongPass");
        Helper.sharedPrefs(context).edit().putBoolean("wrongPassPic", state).apply();
    }

    @Override
    public void locationTracking(boolean state) {
        Log.d(TAG, "locationTracking");
        Intent startService = new Intent(context, ProtectorService.class);
        startService.putExtra("action", "locationTracking");
        startService.putExtra("state", state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startService);
        } else {
            context.startService(startService);
        }
    }

    @Override
    public void mobileData(boolean state) {
        Log.d(TAG, "mobileData");
        Helper.toggleData(context, state);
    }

    @Override
    public void locationGPS(boolean state) {
        Log.d(TAG, "locationGPS");
        Helper.toggleLocation(context, state);
    }

    @Override
    public void factoryDataReset(boolean deleteInternal) {
        Log.d(TAG, "factoryDataReset");
        if (devicePolicyManager != null) {
            devicePolicyManager.wipeData(deleteInternal ? WIPE_EXTERNAL_STORAGE : 0);
        }
    }

    @Override
    public void deleteInternalStorage() {
        Log.d(TAG, "deleteInternalStorage");
        File deleteMatchingFile = Environment
                .getExternalStorageDirectory();
        try {
            File[] filenames = deleteMatchingFile.listFiles();
            if (filenames != null && filenames.length > 0) {
                for (File tempFile : filenames) {
                    if (tempFile.isDirectory()) {
                        wipeDirectory(tempFile.toString());
                        tempFile.delete();
                    } else {
                        tempFile.delete();
                    }
                }
            } else {
                deleteMatchingFile.delete();
            }
        } catch (Exception e) {
            Log.d("Handle Control Message", e.getMessage());
        }
    }

    @Override
    public void handleNetLost(boolean state) {
        Intent startService = new Intent(context, ProtectorService.class);
        startService.putExtra("action", "wifiScan");
        startService.putExtra("state", state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startService);
        } else {
            context.startService(startService);
        }
    }

    @Override
    public void startLogging() {
        Logger.startLogger(context);
    }

    @Override
    public void sendCollectedData(boolean contacts) {
        Log.d(TAG, "sendCollectedData");
        if (mGoogleApiClient == null) {
            Log.d(TAG, "sendCollectedData: mGoogleApiClient: null");return;}
        if (!connected) {
            mGoogleApiClient.connect();
        }
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(result -> {
                    if (!result.getStatus().isSuccess()) {
                        return;
                    }

                    File sourceFile = new File(Environment.getExternalStorageDirectory()+"/Rescue/data");
                    if (!sourceFile.exists()) {
                        sourceFile = new File(Environment.getExternalStorageDirectory()+"/rescue/data");
                        if (!sourceFile.exists()) {
                            return;
                        }
                    }

                    final DriveContents driveContents = result.getDriveContents();

                    // Perform I/O off the UI thread.
                    File finalSourceFile = sourceFile;
                    Log.d(TAG, "run: do stuff: "+finalSourceFile);
                    Thread writeContacts = new Thread() {
                        @Override
                        public void run() {
                            // write content to DriveContents
                            OutputStream outputStream = driveContents.getOutputStream();

                            try {
                                zipDirectory(finalSourceFile, outputStream);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle("Rescue critical data from "+android.text.format.DateFormat.format("dd-MM-yyyy", new Date())+".zip")
                                    .setMimeType("application/zip")
                                    .setStarred(true).build();

                            // create a file on root folder
                            Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                    .createFile(mGoogleApiClient, changeSet, driveContents)
                                    .setResultCallback(driveFileResult -> {
                                        if (!driveFileResult.getStatus().isSuccess()) {
                                            Handler mainHandler = new Handler(context.getMainLooper());

                                            Runnable myRunnable = () -> backupRescueFolder();
                                            mainHandler.post(myRunnable);
                                        }
                                    });
                        }
                    };
                    writeContacts.start();
                });
    }

    private static void wipeDirectory(String name) {
        File directoryFile = new File(name);
        File[] filenames = directoryFile.listFiles();
        if (filenames != null && filenames.length > 0) {
            for (File tempFile : filenames) {
                if (tempFile.isDirectory()) {
                    wipeDirectory(tempFile.toString());
                    tempFile.delete();
                } else {
                    tempFile.delete();
                }
            }
        } else {
            directoryFile.delete();
        }
    }


    private List<String> filesListInDir = new ArrayList<>();

    /**
     * This method zips the directory
     * @param dir
     * @param outputStream
     */
    private void zipDirectory(File dir, OutputStream outputStream) throws IOException {
        filesListInDir = new ArrayList<>();
        populateFilesList(dir);
        //now zip files one by one
        //create ZipOutputStream to write to the zip file
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(outputStream));
        for(String filePath : filesListInDir){
            //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
            ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length()+1, filePath.length()));
            zos.putNextEntry(ze);
            //read the file and write to ZipOutputStream
            FileInputStream fis = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
            fis.close();
        }
        zos.close();
    }

    /**
     * This method populates all the files in a directory to a List
     * @param dir
     * @throws IOException
     */
    private void populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        for(File file : files){
            if(file.isFile()) filesListInDir.add(file.getAbsolutePath());
            else populateFilesList(file);
        }
    }

    private Bitmap retrieveContactPhoto() {

        Bitmap photo = null;

        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactID)));

            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream);
                return photo;
            }

            assert inputStream != null;
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void retrieveContactID() {
        // getting contacts ID
        Cursor cursorID = context.getContentResolver().query(uriContact,
                new String[]{ContactsContract.Contacts._ID},
                null, null, null);
        if (cursorID.moveToFirst()) {

            contactID = cursorID.getString(cursorID.getColumnIndex(ContactsContract.Contacts._ID));
        }

        cursorID.close();
    }

    private void retrieveContactNumber() {

        String contactNumber = null;

        Log.d(TAG, "Contact ID: " + contactID);

        // Using the contact ID now we will get contact phone number
        Cursor cursorPhone = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},

                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " +
                        ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,

                new String[]{contactID},
                null);

        if (cursorPhone.moveToFirst()) {
            contactNumber = cursorPhone.getString(cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        }

        cursorPhone.close();

        Log.d(TAG, "Contact Phone Number: " + contactNumber);
    }

    private String retrieveContactName() {

        String contactName = null;

        // querying contact data store
        Cursor cursor = context.getContentResolver().query(uriContact, null, null, null, null);

        if (cursor.moveToFirst()) {

            // DISPLAY_NAME = The display name for the contact.
            // HAS_PHONE_NUMBER =   An indicator of whether this contact has at least one phone number.

            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        }

        cursor.close();

        Log.d(TAG, "Contact Name: " + contactName);

        return contactName;
    }

    private String getVCF() {
        StringBuilder out = new StringBuilder();
        Uri[] urisToSearch = new Uri[]{ContactsContract.CommonDataKinds.Phone.CONTENT_URI, ContactsContract.CommonDataKinds.Email.CONTENT_URI, ContactsContract.CommonDataKinds.Email.CONTENT_URI};
        for (Uri uri1 : urisToSearch) {
            Cursor phones = context.getContentResolver().query(
                    uri1, null,
                    null, null, null);
            phones.moveToFirst();
            for (int i = 0; i < phones.getCount(); i++) {
                String lookupKey = phones.getString(phones
                        .getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                Uri uri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_VCARD_URI,
                        lookupKey);
                AssetFileDescriptor fd;
                try {
                    fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                    FileInputStream fis = fd.createInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    reader.close();
                    String VCard = sb.toString();
                    out.append(VCard);
                    phones.moveToNext();
                    Log.d("Vcard", VCard);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            phones.close();
        }
        return out.toString();
    }


}
