package ml.coinlist.android;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import java.util.Locale;

public abstract class BaseActivity extends Activity {
    protected SharedPreferences settings;

    public void attachBaseContext(Context context) {
        // Get settings
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

        // Get selected language and theme
        int language = settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT);
        int theme = settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT);

        // Check if they differ from system defaults or when in battery saver mode
        if (
            language != Config.SETTINGS_LANGUAGE_SYSTEM ||
            theme != Config.SETTINGS_THEME_SYSTEM ||
            (theme == Config.SETTINGS_THEME_SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        ) {
            // Create a new updated configuration
            Configuration configuration = new Configuration(context.getResources().getConfiguration());

            // Force a language
            if (language == Config.SETTINGS_LANGUAGE_ENGLISH) {
                configuration.setLocale(new Locale("en"));
            }

            if (language == Config.SETTINGS_LANGUAGE_DUTCH) {
                configuration.setLocale(new Locale("nl"));
            }

            // Force a UI night mode
            if (theme == Config.SETTINGS_THEME_LIGHT) {
                configuration.uiMode |= Configuration.UI_MODE_NIGHT_NO;
                configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_YES;
            }

            if (theme == Config.SETTINGS_THEME_DARK) {
                configuration.uiMode |= Configuration.UI_MODE_NIGHT_YES;
                configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_NO;
            }

            // Or set dark mode on when in battery saver mode
            if (theme == Config.SETTINGS_THEME_SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isPowerSaveMode()) {
                    configuration.uiMode |= Configuration.UI_MODE_NIGHT_YES;
                    configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_NO;
                } else {
                    configuration.uiMode |= Configuration.UI_MODE_NIGHT_NO;
                    configuration.uiMode &= ~Configuration.UI_MODE_NIGHT_YES;
                }
            }

            // Update the context
            super.attachBaseContext(context.createConfigurationContext(configuration));
            return;
        }

        // Use the default context
        super.attachBaseContext(context);
    }
}
