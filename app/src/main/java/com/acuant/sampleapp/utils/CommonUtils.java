package com.acuant.sampleapp.utils;

import com.acuant.acuantdocumentprocessing.healthinsuranceresultmodel.HealthInsuranceCardResult;

import java.lang.reflect.Field;
import java.util.Objects;

public class CommonUtils {

    //This method is just a quick example of how to get some of the basic info. There are many
    // fields that would not be covered by this. In a real implementations you should pick each
    // field individually
    public static String stringFromHealthInsuranceResult(HealthInsuranceCardResult result){
        StringBuilder str = new StringBuilder();
        Field [] allFields = null;
        if (result != null) {
            allFields = HealthInsuranceCardResult.class.getDeclaredFields();
        }
        if (allFields != null && allFields.length > 0) {
            for (Field field : allFields) {
                try {
                    field.getName();
                    if (!field.getName().startsWith("rawText") && !field.getName().startsWith("frontImageString") && !field.getName().startsWith("backImageString") && String.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        if (field.get(result) != null && Objects.requireNonNull(field.get(result)).toString().trim().length()>0) {
                            str.append(field.getName()).append(": ").append(field.get(result)).append(System.lineSeparator());
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return str.toString();
    }
}
