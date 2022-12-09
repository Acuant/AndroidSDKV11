package com.acuant.acuantfacecapture.language;


import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleManager {

    public static Context updateResources(Context context, final Locale locale) {
        final Resources res = context.getResources();
        final Configuration config = new Configuration(res.getConfiguration());
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= 17) {

            config.setLocale(locale);
            context = context.createConfigurationContext(config);
            // I need to set this so that recall setText(R.string.xxxx) works
            res.updateConfiguration(config, res.getDisplayMetrics());
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        return context;
    }

}

