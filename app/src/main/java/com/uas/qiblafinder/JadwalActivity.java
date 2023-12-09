package com.uas.qiblafinder;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;

public class JadwalActivity extends AppCompatActivity {
    private ImageButton btnJadwalBack;
    private TextView textSubuh, textZuhur, textAshar, textMaghrib, textIsya;
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jadwal);

        btnJadwalBack = (ImageButton) findViewById(R.id.btnJadwalBack);

        textSubuh = (TextView) findViewById(R.id.textSubuh);
        textZuhur = (TextView) findViewById(R.id.textZuhur);
        textAshar = (TextView) findViewById(R.id.textAshar);
        textMaghrib = (TextView) findViewById(R.id.textMaghrib);
        textIsya = (TextView) findViewById(R.id.textIsya);

        LocalDate date = LocalDate.now();
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        gps = new GPSTracker(this);
        if (gps.canGetLocation()) {
            double lat = gps.getLatitude();
            double lon = gps.getLongitude();

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String url = String.format("https://api.aladhan.com/v1/calendar/%d/%d?latitude=%f&longitude=%f&method=99", year, month, lat, lon);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONArray jsonArray = response.getJSONArray("data");
                        JSONObject jsonObject = jsonArray.getJSONObject(day - 1);
                        JSONObject data = jsonObject.getJSONObject("timings");

                        textSubuh.setText(data.getString("Fajr"));
                        textZuhur.setText(data.getString("Dhuhr"));
                        textAshar.setText(data.getString("Asr"));
                        textMaghrib.setText(data.getString("Maghrib"));
                        textIsya.setText(data.getString("Isha"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(JadwalActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            requestQueue.add(jsonObjectRequest);
        }

        btnJadwalBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}