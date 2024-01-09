package com.example.boatparis;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class list_monument extends AppCompatActivity {

    private ListView listView;
    private static MaBaseSQLite maBaseSQLite;
    private SQLiteDatabase db;
    private ArrayAdapter<String> adapter;
    private Button btnGoToOtherActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_monument);
        Log.d("MONUMENT ACTIVITY","APP CREATE");
        listView = findViewById(R.id.listview);

        maBaseSQLite = new MaBaseSQLite(this, "DB", null, 1);
        btnGoToOtherActivity = findViewById(R.id.button2);
        btnGoToOtherActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Code pour passer à une autre activité
                Intent intent = new Intent(list_monument.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
    public static class MaBaseSQLite extends SQLiteOpenHelper {

        public static final String COL_ADDRESS = "addresse";
        public static final String COL_CARTO = "nom_carto";
        static final String TABLE_NAME = "table_currency";
        private static final String COL_LAT = "LATITUDE";
        private static final String COL_LON = "LONGITUDE";
        private static final String COL_ID = "ID";


        private static final String CREATE_BDD = "CREATE TABLE " + TABLE_NAME + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_ADDRESS + " TEXT NOT NULL, "
                + COL_CARTO + " TEXT ,"+COL_LON + " DOUBLE NOT NULL, "+COL_LAT + " DOUBLE NOT NULL); ";

        public MaBaseSQLite(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_BDD);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE " + TABLE_NAME + ";");
            onCreate(db);
        }
    }

    private class CheckConnectionTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int code = connection.getResponseCode();
                return code == 200;
            } catch (IOException e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                Log.d("CONNEXION :", "Connexion au serveur de la BCE réussie.");
                //title.setText(R.string.txt);
                updateDatabase();
            } else {
                Log.d("CONNEXION :", "Erreur de connexion au serveur de la BCE.");
                //title.setText(R.string.err_co);
                loadFromDatabase();
            }
        }
    }

    private void updateDatabase() {
        db = maBaseSQLite.getWritableDatabase();
        db.execSQL("DELETE FROM " + MaBaseSQLite.TABLE_NAME);

        for (Map.Entry<String, Map<String, Object>> entry : MainActivity.resultMap.entrySet()) {
            ContentValues values = new ContentValues();
            values.put(MaBaseSQLite.COL_ADDRESS, entry.getValue().toString());
            values.put(MaBaseSQLite.COL_CARTO, entry.getValue().toString());
            values.put(MaBaseSQLite.COL_LON, entry.getKey());
            values.put(MaBaseSQLite.COL_LAT, entry.getKey());
            db.insert(MaBaseSQLite.TABLE_NAME, null, values);
        }

        loadFromDatabase();
    }

    private void loadFromDatabase() {
        db = maBaseSQLite.getReadableDatabase();
        Cursor c = db.query(MaBaseSQLite.TABLE_NAME, new String[] {MaBaseSQLite.COL_ADDRESS, MaBaseSQLite.COL_CARTO, MaBaseSQLite.COL_LON, MaBaseSQLite.COL_LAT}, null, null, null, null, null);

        List<String> resultList = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : MainActivity.resultMap.entrySet()) {
            // Construisez la chaîne à afficher dans la ListView
            String data = "ID: " + entry.getKey() + "\n";
            Map<String, Object> locationMap = entry.getValue();
            data += "Latitude: " + locationMap.get("latitude") + "\n";
            data += "Longitude: " + locationMap.get("longitude") + "\n";
            data += "Adresse: " + locationMap.get("adresse") + "\n";
            data += "Nom Carto: " + locationMap.get("nom_carto") + "\n\n";

            resultList.add(data);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultList);
        listView.setAdapter(adapter);
    }

    public void onStart() {
        super.onStart();
        new CheckConnectionTask().execute("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml");
        Log.d("SQL ","UPDATE FROM WEB");
        Log.d("MONUMENT ACTIVITY","APP START");
    }

    public void onPause() {
        super.onPause();
        loadFromDatabase();
        Log.d("MONUMENT ACTIVITY","APP STOP");
    }

    public void onResume() {
        super.onResume();
        loadFromDatabase();
        Log.d("MONUMENT ACTIVITY","APP RESUME");
    }
}
