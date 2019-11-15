package com.pushdy.core.ultilities;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class PDYSystemUtil {
    private static String DEVICE_UUID;
    private static String DEVICE_FILE = ".pushdy_device.dat";
    private static String DEVICE_UUID_KEY = "PUSHDY_DEVICE_UUID_KEY";
    private static String UNKNOWN_DEVICE_UUID = "unknown";

    public static String getUUID(Context context) {
        context = context.getApplicationContext();
        if (context != null) {
            if (DEVICE_UUID == null || DEVICE_UUID.equals("") || DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID)) {
                DEVICE_UUID = PDYStorage.getString(context, DEVICE_UUID_KEY);

                if (DEVICE_UUID == null || DEVICE_UUID.equals("") || DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID)) {
                    try {
                        DEVICE_UUID = readFromFile(context, DEVICE_FILE);
                    } catch (Exception e) {
                    }
                }

                if (DEVICE_UUID == null || DEVICE_UUID.equals("") || DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID)) {
                    try {
                        TelephonyManager tManager = (TelephonyManager)
                                context.getSystemService(Context.TELEPHONY_SERVICE);
                        DEVICE_UUID = tManager.getDeviceId();
                    } catch (Exception e) {
                    }
                }

                // If the ID does not available (on tablets that do not have phone
                // function)
                // Then try to retrieve hardware S/N instead:
                if (DEVICE_UUID == null || DEVICE_UUID.equals("") || DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID)) {
                    try {
                        DEVICE_UUID = Build.class.getField("SERIAL").get(null).toString();
                    } catch (Exception e) {
                    }
                }

                // When even hardware S/N not found, then we use the Android unique
                // ID as below:
                // *** Note that: this ID will be changed when user do a hard reset,
                // and in some case, due to manufacture problems, they may use the
                // same ID for all devices in the same series.
                if (DEVICE_UUID == null || DEVICE_UUID.equals("") || DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID)) {
                    try {
                        DEVICE_UUID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                    } catch (Exception e) {
                    }
                }

                if (DEVICE_UUID == null || DEVICE_UUID.equals("") || DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID)) {
                    try {
                        DEVICE_UUID = UUID.randomUUID().toString();
                    } catch (Exception e) {
                    }
                }

                if (DEVICE_UUID != null && !DEVICE_UUID.equals("") && !DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID)) {
                    // Remove null string if have
                    DEVICE_UUID = DEVICE_UUID.replace("\\u0000", "");
                    if (DEVICE_UUID != null && !DEVICE_UUID.equals("") && !DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID)) {
                        PDYStorage.setString(context, DEVICE_UUID_KEY, DEVICE_UUID);
                        try {
                            writeToFile(context, DEVICE_UUID, DEVICE_FILE);
                        } catch (Exception e) {
                        }
                    }
                }
            } else {
                // Remove null string if have
                DEVICE_UUID = DEVICE_UUID.replace("\\u0000", "");
            }
        }
        return DEVICE_UUID != null && !DEVICE_UUID.equals("") && !DEVICE_UUID.equalsIgnoreCase(UNKNOWN_DEVICE_UUID) ? DEVICE_UUID : "";
    }

    private static String readFromFile(Context context, String fileName) {
        String ret = "";
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory(), fileName)));
            while ((line = in.readLine()) != null) stringBuilder.append(line);
            ret = stringBuilder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static String writeToFile(Context context, String data, String fileName) {
        try {
            boolean isSDPresent = android.os.Environment.getExternalStorageState().
                    equals(android.os.Environment.MEDIA_MOUNTED);

            FileWriter out;

            if (isSDPresent) {//if external storage exist
                out = new FileWriter(new File(Environment.getExternalStorageDirectory(), fileName), false);

            } else {
                // If external storage is not exist, write to internal storage
                out = new FileWriter(new File(context.getApplicationContext().getFilesDir(),
                        fileName), false);
            }

            out.write(data);
            out.close();
            return data;
        } catch (IOException e) {
            return "";
        }
    }
}
