package com.acuant.sampleapp;

import android.graphics.Bitmap;

/**
 * Created by tapasbehera on 5/16/18.
 */

public class ProcessedData {
    public static Bitmap frontImage = null;
    public static Bitmap backImage = null;
    public static Bitmap faceImage = null;
    public static Bitmap signImage = null;
    public static String formattedString="";
    public static String dateOfBirth = null;
    public static String dateOfExpiry = null;
    public static String documentNumber = null;
    public static boolean isHealthCard = false;
    public static String cardType = "ID1";
    public static void cleanup(){
        frontImage = null;
        backImage = null;
        faceImage = null;
        signImage = null;
        formattedString="";
        dateOfBirth = null;
        dateOfExpiry = null;
        documentNumber = null;
        isHealthCard = false;
        cardType = "ID1";
    }
}
