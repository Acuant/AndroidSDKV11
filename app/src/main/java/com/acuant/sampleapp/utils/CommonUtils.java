package com.acuant.sampleapp.utils;

import android.graphics.Bitmap;
import android.os.Environment;

import com.acuant.acuantfacematchsdk.model.FacialMatchResult;
import com.acuant.acuantdocumentprocessing.model.HealthInsuranceCardResult;
import com.acuant.acuantdocumentprocessing.model.ProcessingResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by tapasbehera on 5/16/18.
 */

public class CommonUtils {
    public static String stringFromResult(ProcessingResult result){
        String str = "";
        Field [] allFields = null;
        if(result instanceof HealthInsuranceCardResult){
            allFields = HealthInsuranceCardResult.class.getDeclaredFields();
        }
        if(allFields!=null && allFields.length>0) {
            for (Field field : allFields) {
                try {
                    if (field.getName() != null && !field.getName().startsWith("kCard")
                    && !field.getName().startsWith("kDriversCard")
                            && !field.getName().startsWith("kAuth")
                    && (String.class.isAssignableFrom(field.getType()))) {
                        field.setAccessible(true);
                        if (field.get(result) != null && field.get(result).toString().trim().length()>0) {
                            str = str + field.getName() + ":" + field.get(result) + System.lineSeparator();
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return str;
    }

    public static String stringFromFacialMatchResult(FacialMatchResult result){
        String str = "";
        Field [] allFields = FacialMatchResult.class.getDeclaredFields();
        if(allFields!=null && allFields.length>0) {
            for (Field field : allFields) {
                try {
                    if (field.getName() != null && !field.getName().startsWith("kCard")) {
                        field.setAccessible(true);
                        if (field.get(result) != null) {
                            str = str + field.getName() + ":" + field.get(result) + System.lineSeparator();
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return str;
    }

    public static String getDisplayFromattedStringFromDateString(int year,int month,int day){
        String retString = null;
        Date date = new Date(year, month, day);
        SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd");
        retString = sdf.format(date);
        return retString;
    }

    public static String getInMMddyyFormat(int year,int month,int day){
        String retString = null;
        Date date = new Date(year, month, day);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy");
        retString = sdf.format(date);
        return retString;
    }

    public static int get4DigitYear(int year,int month,int day){
        Date date = new Date(year, month, day);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        int ret_year = Integer.parseInt(sdf.format(date).split("/")[0]);
        return ret_year;
    }

    public static  void saveImage(Bitmap bmp,String fileName){

        try {
            String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/sdcard/"+fileName+".png";
            File file = new File(file_path);
            file.getParentFile().mkdirs();
            if(file.exists()==false) {
                file.createNewFile();
            }
            FileOutputStream fOut = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
