package com.example.boatparis;

import static com.example.boatparis.MainActivity.latLng;
import static com.example.boatparis.MainActivity.resultMap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class info_page extends AppCompatActivity {
    private static int Earth_Rayon = 6371000;
    private TextView neardest;

    private Button btn2map;

    int hashMapSize = resultMap.size();
    public Object[][] Near_comparison = new Object[hashMapSize][5];


    private static TextView chatgpt;
    private String ChatGPT_url = "https://api.openai.com/v1/chat/completions";

    private String API_KEY = "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";

    private String Near_monument;
    private String ChatGPT_out="";
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("CHAT GPT ACTIVITY","APP CREATE");
        setContentView(R.layout.activity_info_page);
        neardest = findViewById(R.id.near_dest);
        btn2map = findViewById(R.id.button2);
        chatgpt = findViewById(R.id.chatgpt);

        btn2map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Code pour passer à une autre activité
                Intent intent = new Intent(info_page.this, MainActivity.class);
                startActivity(intent);
            }
        });


        int i = 0;
        for (Map.Entry<String, Map<String, Object>> entry : resultMap.entrySet()) {
            Map<String, Object> locationData = entry.getValue();
            double latitude = (double) locationData.get("latitude");
            double longitude = (double) locationData.get("longitude");
            String carto = (String) locationData.get("nom_carto");
            String address = (String) locationData.get("adresse");

            double latARad = Math.toRadians(latLng.latitude);
            double longARad = Math.toRadians(latLng.longitude);
            double latBRad = Math.toRadians(latitude);
            double longBRad = Math.toRadians(longitude);
            double c = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((latBRad - latARad) / 2), 2) + Math.cos(latARad) * Math.cos(latBRad) * Math.pow(Math.sin((longBRad - longARad) / 2), 2)));
            double neardistance = c * Earth_Rayon;
            ajouterLigne(Near_comparison, i, address, carto, latitude, longitude, neardistance);
            i++;
        }
    }
    public void button_ChatGPT(View view) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-3.5-turbo");

            JSONArray jsonArrayMessage = new JSONArray();
            JSONObject jsonObjectMessage = new JSONObject();

            jsonObjectMessage.put("role", "user");
            jsonObjectMessage.put("content", "Give me a description of "+Near_monument+" in five line maximum for tourist"); // Fix here

            jsonArrayMessage.put(jsonObjectMessage);
            jsonObject.put("messages", jsonArrayMessage); // Fix here

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                ChatGPT_url, jsonObject,new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String StringText = null;
                try {
                    StringText = response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                ChatGPT_out = ChatGPT_out + StringText;
                chatgpt.setText(ChatGPT_out);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public Map <String , String> getHeaders() throws AuthFailureError{
                Map <String , String> mapHeader = new HashMap<>();
                mapHeader.put("Authorization","Bearer "+API_KEY);
                mapHeader.put("Content-Type", "application/json");
                return mapHeader;
            }
            @Override
            protected Response <JSONObject> parseNetworkResponse(NetworkResponse response){
                return super.parseNetworkResponse(response);
            }
        };

        int intTimeoutPeriod = 60000; // 60 seconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(intTimeoutPeriod
                ,DefaultRetryPolicy.DEFAULT_MAX_RETRIES
                ,DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        Volley.newRequestQueue(getApplicationContext()).add(jsonObjectRequest);

    }

    public void onStart() {
        super.onStart();
        Log.d("CHAT GPT ACTIVITY","APP START");
        int nearest = findMinRowIndex(Near_comparison, 4);

        Near_monument = (String) Near_comparison[nearest][1];
        double Near_distance = (double) Near_comparison[nearest][4];
        String unit = "";
        String formattedDistance;

        if (Near_distance > 1000) {
            Near_distance /= 1000;
            formattedDistance = String.format("%.2f", Near_distance);
            unit = "KM";
        } else {
            unit = "M";
            formattedDistance = String.format("%.0f", Near_distance);
        }

        String finalUnit = unit;

        neardest.setText(Near_monument + "\n" + formattedDistance + " " + finalUnit+" de vous");




        Log.d("HashMapSize", "Taille de la HashMap : " + hashMapSize);
    }



    // Méthode pour ajouter une ligne au tableau
    private static void ajouterLigne(Object[][] tableau, int ligneIndex, Object c1, Object c2, Object c3, Object c4, Object c5) {
        // Vérifier si l'index de ligne est valide
        if (ligneIndex >= 0 && ligneIndex < tableau.length) {
            tableau[ligneIndex][0] = c1;
            tableau[ligneIndex][1] = c2;
            tableau[ligneIndex][2] = c3;
            tableau[ligneIndex][3] = c4;
            tableau[ligneIndex][4] = c5;
        } else {
            System.out.println("Index de ligne invalide");
        }
    }

    // Méthode pour trouver l'indice de la ligne avec la plus petite valeur dans une colonne spécifique
    public static int findMinRowIndex(Object[][] tableau, int colonne) {
        if (tableau.length == 0 || colonne < 0 || colonne >= tableau[0].length) {
            throw new IllegalArgumentException("Tableau invalide ou indice de colonne incorrect");
        }

        double minValeur = (double) tableau[0][colonne];
        int minRowIndex = 0;

        for (int i = 1; i < tableau.length; i++) {
            if ((double) tableau[i][colonne] < minValeur) {
                minValeur = (double) tableau[i][colonne];
                minRowIndex = i;
            }
        }

        return minRowIndex;
    }

    public void onPause() {
        super.onPause();
        Log.d("CHAT GPT ACTIVITY","APP PAUSE");
    }

    public void onResume() {
        super.onResume();
        Log.d("CHAT GPT ACTIVITY","APP RESUME");
    }



}
