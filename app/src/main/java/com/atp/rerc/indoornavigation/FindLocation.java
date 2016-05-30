package com.atp.rerc.indoornavigation;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;

public class FindLocation extends AppCompatActivity {

    IALocationManager mIALocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_location);
        mIALocationManager = IALocationManager.create(this);
    }

    public void buttonFindLocation(View v) {
        startActivity(new Intent(FindLocation.this, MapsActivity.class));
    }

    public void buttonMapsOverlay(View v) {
        startActivity(new Intent(FindLocation.this, MapsOverlayActivity.class));
    }

    public void buttonImageView(View v) {
        startActivity(new Intent(FindLocation.this, ImageViewActivity.class));
    }


    private IALocationListener mIALocationListener = new IALocationListener() {

        // Called when the location has changed

        @Override
        public void onLocationChanged(IALocation location) {

            Log.d("Latitude", "Latitude: " + location.getLatitude());
            Log.d("Longitude", "Longitude: " + location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(),mIALocationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIALocationManager.removeLocationUpdates(mIALocationListener);
    }

    @Override
    protected void onDestroy() {
        mIALocationManager.destroy();
        super.onDestroy();
    }
}
