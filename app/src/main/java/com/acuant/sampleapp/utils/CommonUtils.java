package com.acuant.sampleapp.utils;

import com.acuant.acuantdocumentprocessing.resultmodel.HealthInsuranceCardResult;
import java.lang.reflect.Field;
import java.util.Objects;

public class CommonUtils {
    public static String stringFromHealthInsuranceResult(HealthInsuranceCardResult result){
        String str = "";
        Field [] allFields = null;
        if (result != null) {
            allFields = HealthInsuranceCardResult.class.getDeclaredFields();
        }
        if (allFields != null && allFields.length > 0) {
            for (Field field : allFields) {
                try {
                    field.getName();
                    if (!field.getName().startsWith("kCard") && !field.getName().startsWith("kDriversCard") && !field.getName().startsWith("kAuth") && String.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        if (field.get(result) != null && Objects.requireNonNull(field.get(result)).toString().trim().length()>0) {
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
}
