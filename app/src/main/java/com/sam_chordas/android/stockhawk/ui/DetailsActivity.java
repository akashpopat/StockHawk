package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetailsActivity extends AppCompatActivity {

    String mSymbol;
    LineChart mChart;
    ProgressBar mBar;
    private String companyName;
    private ArrayList<String> labels;
    private ArrayList<Float> values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        mSymbol = getIntent().getStringExtra("stock");
        setTitle(mSymbol);

        mChart = (LineChart) findViewById(R.id.chart);
        mBar = (ProgressBar) findViewById(R.id.progressBar);

        downloadStockDetails();
    }

    private void downloadStockDetails() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://chartapi.finance.yahoo.com/instrument/1.0/" + mSymbol + "/chartdata;type=quote;range=5y/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onDownloadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) { //on Success
                    try {

                        String result = response.body().string();
                        if (result.startsWith("finance_charts_json_callback( ")) {
                            result = result.substring(29, result.length() - 2);
                        }


                        JSONObject object = new JSONObject(result);
                        companyName = object.getJSONObject("meta").getString("Company-Name");
                        labels = new ArrayList<>();
                        values = new ArrayList<>();
                        JSONArray series = object.getJSONArray("series");
                        for (int i = 0; i < series.length(); i++) {
                            JSONObject seriesItem = series.getJSONObject(i);
                            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyyMMdd");
                            String date = android.text.format.DateFormat.
                                    getMediumDateFormat(getApplicationContext()).
                                    format(srcFormat.parse(seriesItem.getString("Date")));
                            labels.add(date);
                            values.add(Float.parseFloat(seriesItem.getString("close")));
                        }

                        onDownloadCompleted();
                    } catch (Exception e) {
                        onDownloadFailed();
                        e.printStackTrace();
                    }
                } else {
                    onDownloadFailed();
                }
            }
        });
    }
    private void onDownloadCompleted() {

        List<Entry> entries = new ArrayList<Entry>();
        for(int i = 0; i < labels.size(); i++){
            entries.add(new Entry(i,values.get(i)));
        }
        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.five_years_weekly));
        dataSet.setColor(R.color.material_blue_500);
        dataSet.setValueTextColor(R.color.material_green_700);
        LineData lineData = new LineData(dataSet);
        mChart.setData(lineData);
        mChart.invalidate(); // refresh
        DetailsActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBar.setVisibility(View.GONE);
                mChart.setVisibility(View.VISIBLE);
            }
        });
    }
    private void onDownloadFailed() {
        DetailsActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBar.setVisibility(View.GONE);
                mChart.setVisibility(View.VISIBLE);
            }
        });
    }
}
