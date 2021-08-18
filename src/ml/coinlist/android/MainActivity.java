package ml.coinlist.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends BaseActivity {
    public static final int SETTINGS_REQUEST_CODE = 1;

    private Handler handler;
    private int oldLanguage = -1;
    private int oldTheme = -1;

    private boolean starredOnly;
    private ListView coinsList;
    private CoinsAdapter coinsAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        // Init starred button
        ImageButton starredButton = (ImageButton)findViewById(R.id.main_starred_button);

        starredOnly = settings.getBoolean("starred_only", Config.SETTINGS_STARRED_ONLY_DEFAULT);
        if (starredOnly) {
            starredButton.setImageResource(R.drawable.ic_star);
        } else {
            starredButton.setImageResource(R.drawable.ic_star_outline);
        }

        starredButton.setOnClickListener(view -> {
            starredOnly = !starredOnly;

            if (starredOnly) {
                starredButton.setImageResource(R.drawable.ic_star);
            } else {
                starredButton.setImageResource(R.drawable.ic_star_outline);
            }

            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putBoolean("starred_only", starredOnly);
            settingsEditor.apply();

            loadCoins();
        });

        // Init refresh button
        ((ImageButton)findViewById(R.id.main_refresh_button)).setOnClickListener(view -> {
            loadCoins();
        });

        // Init settings button
        ((ImageButton)findViewById(R.id.main_settings_button)).setOnClickListener(view -> {
            oldLanguage = settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT);
            oldTheme = settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT);
            startActivityForResult(new Intent(this, SettingsActivity.class), MainActivity.SETTINGS_REQUEST_CODE);
        });

        // Load coins data
        coinsList = (ListView)findViewById(R.id.main_coins_list);
        coinsAdapter = new CoinsAdapter(this);
        coinsList.setAdapter(coinsAdapter);
        coinsList.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {
            Coin coin = coinsAdapter.getItem(position);
            if (coin.getExtraIndex() == 2) {
                coin.setExtraIndex(0);
            } else {
                coin.setExtraIndex(coin.getExtraIndex() + 1);
            }
            coinsAdapter.notifyDataSetChanged();
        });

        loadCoins();
    }

    // When come back of the settings activity check for restart
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.SETTINGS_REQUEST_CODE) {
            if (oldLanguage != -1 && oldTheme != -1) {
                if (
                    oldLanguage != settings.getInt("language", Config.SETTINGS_LANGUAGE_DEFAULT) ||
                    oldTheme != settings.getInt("theme", Config.SETTINGS_THEME_DEFAULT)
                ) {
                    handler.post(() -> {
                        recreate();
                    });
                }
            }
        }
    }

    // Load coin information
    private void loadCoins() {
        coinsAdapter.clear();
        FetchDataTask.with(this).load("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd").then(data -> {
            try {
                JSONArray jsonStarredCoins = new JSONArray(settings.getString("starred_coins", "[]"));
                JSONArray jsonCoins = new JSONArray(data);
                for (int i = 0; i < jsonCoins.length(); i++) {
                    JSONObject jsonCoin = jsonCoins.getJSONObject(i);

                    boolean isStarred = false;
                    for (int j = 0; j < jsonStarredCoins.length(); j++) {
                        if (jsonCoin.getString("id").equals(jsonStarredCoins.getString(j))) {
                            isStarred = true;
                            break;
                        }
                    }
                    if (starredOnly && !isStarred) {
                        continue;
                    }

                    coinsAdapter.add(new Coin(
                        jsonCoin.getString("id"),
                        jsonCoin.getInt("market_cap_rank"),
                        jsonCoin.getString("name"),
                        jsonCoin.getString("image"),
                        jsonCoin.getDouble("current_price"),
                        jsonCoin.getDouble("price_change_percentage_24h"),
                        jsonCoin.getDouble("market_cap"),
                        jsonCoin.getDouble("total_volume"),
                        jsonCoin.getDouble("circulating_supply"),
                        isStarred
                    ));
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }).fetch();
    }
}
