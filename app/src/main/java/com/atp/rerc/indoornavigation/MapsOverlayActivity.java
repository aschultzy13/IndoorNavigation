package com.atp.rerc.indoornavigation;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.PolyUtil;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

public class MapsOverlayActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    private static final String TAG = "IndoorAtlasExample";

    private static final float HUE_IABLUE = 200.0f;

    /* used to decide when bitmap should be downscaled */
    private static final int MAX_DIMENSION = 2048;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Marker mMarker;
    private GroundOverlay mGroundOverlay;
    private IALocationManager mIALocationManager;
    private IAResourceManager mResourceManager;
    private IATask<IAFloorPlan> mFetchFloorPlanTask;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating;

    /**
     * Listener that handles location change events.
     */
    private IALocationListener mListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */
        @Override
        public void onLocationChanged(IALocation location) {

            // lat long coordinates of colorado 4 corners
//            double latitude[] = {41.00414793516995,  40.99628891284563, 36.98337823446781, 37.00220566162535};
//            double longitude[] = {-109.0776588767767, -102.04488672316073, -102.04244691878557, -109.03938073664904};
            // corners of the hub
            double latitude[] = {39.74368787333355,  39.74294231769039, 39.742607949347864, 39.74335118841613};
            double longitude[] = {-105.01030012965202, -105.00974357128143, -105.0104996189475, -105.01105818897486};
            // put coordinates in 2d arraylist
            List<LatLng> gpsCoordinates = new ArrayList<>();
            for (int i = 0; i < latitude.length; i++){
                gpsCoordinates.add(new LatLng(latitude[i],longitude[i]));
            }

            // point that is inside or outside area of polygon
            // need (compile 'com.google.maps.android:android-maps-utils:0.4+') in gradle dependencies for polyutil
            LatLng testPoint = new LatLng(location.getLatitude(),location.getLongitude());
            boolean answer = PolyUtil.containsLocation(testPoint,gpsCoordinates,true);
            System.out.println("The coordinate is in the polygon: " + answer);

            Log.d(TAG, "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            if (mMap == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mMarker == null) {
                // first location, add marker
                mMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(HUE_IABLUE)));
            } else {
                // move existing markers position to received location
                mMarker.setPosition(latLng);
            }

            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    /**
     * Region listener that when:
     * <ul>
     * <li>region has entered; marks need to move camera and starts
     * loading floor plan bitmap</li>
     * <li>region has existed; clears marker</li>
     * </ul>.
     */
    private IARegion.Listener mRegionListener = new IARegion.Listener() {

        @Override
        public void onEnterRegion(IARegion region) {

            if (region.getType() == IARegion.TYPE_UNKNOWN) {
                Toast.makeText(MapsOverlayActivity.this, "Moved out of map",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // entering new region, mark need to move camera
            mCameraPositionNeedsUpdating = true;

            final String newId = region.getId();

            Toast.makeText(MapsOverlayActivity.this, newId, Toast.LENGTH_SHORT).show();
            fetchFloorPlan(newId);
        }

        @Override
        public void onExitRegion(IARegion region) {
            if (mMarker != null) {
                mMarker.remove();
                mMarker = null;
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_overlay);

        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);

        // instantiate IALocationManager and IAResourceManager
        mIALocationManager = IALocationManager.create(this);
        mResourceManager = IAResourceManager.create(this);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("","mapready");
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remember to clean up after ourselves
        mIALocationManager.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);



        }

        // start receiving location updates & monitor region changes
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }


    /**
     * Sets bitmap of floor plan as ground overlay on Google Maps
     */
    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {

        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
        }

        if (mMap != null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = floorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .position(center, floorPlan.getWidthMeters(), floorPlan.getHeightMeters())
                    .bearing(floorPlan.getBearing());

            mGroundOverlay = mMap.addGroundOverlay(fpOverlay);
        }
    }

    /**
     * Download floor plan using Picasso library.
     */
    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan) {

        final String url = floorPlan.getUrl();

        if (mLoadTarget == null) {
            mLoadTarget = new Target() {

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    Log.d(TAG, "onBitmap loaded with dimensions: " + bitmap.getWidth() + "x"
                            + bitmap.getHeight());
                    setupGroundOverlay(floorPlan, bitmap);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    // N/A
                }

                @Override
                public void onBitmapFailed(Drawable placeHolderDraweble) {
                    Toast.makeText(MapsOverlayActivity.this, "Failed to load bitmap",
                            Toast.LENGTH_SHORT).show();
                }
            };
        }

        RequestCreator request = Picasso.with(this).load(url);

        final int bitmapWidth = floorPlan.getBitmapWidth();
        final int bitmapHeight = floorPlan.getBitmapHeight();

        if (bitmapHeight > MAX_DIMENSION) {
            request.resize(0, MAX_DIMENSION);
        } else if (bitmapWidth > MAX_DIMENSION) {
            request.resize(MAX_DIMENSION, 0);
        }

        request.into(mLoadTarget);
    }


    /**
     * Fetches floor plan data from IndoorAtlas server.
     */
    private void fetchFloorPlan(String id) {

        // if there is already running task, cancel it
        cancelPendingNetworkCalls();

        final IATask<IAFloorPlan> task = mResourceManager.fetchFloorPlanWithId(id);

        task.setCallback(new IAResultCallback<IAFloorPlan>() {

            @Override
            public void onResult(IAResult<IAFloorPlan> result) {

                if (result.isSuccess() && result.getResult() != null) {
                    // retrieve bitmap for this floor plan metadata
                    fetchFloorPlanBitmap(result.getResult());
                } else {
                    // ignore errors if this task was already canceled
                    if (!task.isCancelled()) {
                        // do something with error
                        Toast.makeText(MapsOverlayActivity.this,
                                "loading floor plan failed: " + result.getError(), Toast.LENGTH_LONG)
                                .show();
                        // remove current ground overlay
                        if (mGroundOverlay != null) {
                            mGroundOverlay.remove();
                            mGroundOverlay = null;
                        }
                    }
                }
            }
        }, Looper.getMainLooper()); // deliver callbacks using main looper

        // keep reference to task so that it can be canceled if needed
        mFetchFloorPlanTask = task;

    }

    /**
     * Helper method to cancel current task if any.
     */
    private void cancelPendingNetworkCalls() {
        if (mFetchFloorPlanTask != null && !mFetchFloorPlanTask.isCancelled()) {
            mFetchFloorPlanTask.cancel();
        }
    }


    @Override
    public void onMapClick(LatLng latLng) {
        Toast.makeText(MapsOverlayActivity.this,
                "onMapClick:\n" + latLng.latitude + " : " + latLng.longitude,
                Toast.LENGTH_LONG).show();
        System.out.println("success!!!");
    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        Toast.makeText(MapsOverlayActivity.this,
                "onMapLongClick:\n" + latLng.latitude + " : " + latLng.longitude,
                Toast.LENGTH_LONG).show();

        //Add marker on LongClick position
        MarkerOptions markerOptions =
                new MarkerOptions().position(latLng).title(latLng.toString());
        mMap.addMarker(markerOptions);
    }


    private void addMarker(){
        if(mMap != null){

            //create custom LinearLayout programmatically
            LinearLayout layout = new LinearLayout(MapsOverlayActivity.this);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.VERTICAL);

            final EditText titleField = new EditText(MapsOverlayActivity.this);
            titleField.setHint("Title");

            final EditText latField = new EditText(MapsOverlayActivity.this);
            latField.setHint("Latitude");
            latField.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    | InputType.TYPE_NUMBER_FLAG_SIGNED);

            final EditText lonField = new EditText(MapsOverlayActivity.this);
            lonField.setHint("Longitude");
            lonField.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    | InputType.TYPE_NUMBER_FLAG_SIGNED);

            layout.addView(titleField);
            layout.addView(latField);
            layout.addView(lonField);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Marker");
            builder.setView(layout);
            AlertDialog alertDialog = builder.create();

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean parsable = true;
                    Double lat = null, lon = null;

                    String strLat = latField.getText().toString();
                    String strLon = lonField.getText().toString();
                    String strTitle = titleField.getText().toString();

                    try{
                        lat = Double.parseDouble(strLat);
                    }catch (NumberFormatException ex){
                        parsable = false;
                        Toast.makeText(MapsOverlayActivity.this,
                                "Latitude does not contain a parsable double",
                                Toast.LENGTH_LONG).show();
                    }

                    try{
                        lon = Double.parseDouble(strLon);
                    }catch (NumberFormatException ex){
                        parsable = false;
                        Toast.makeText(MapsOverlayActivity.this,
                                "Longitude does not contain a parsable double",
                                Toast.LENGTH_LONG).show();
                    }

                    if(parsable){

                        LatLng targetLatLng = new LatLng(lat, lon);
                        MarkerOptions markerOptions =
                                new MarkerOptions().position(targetLatLng).title(strTitle);

                        mMap.addMarker(markerOptions);
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(targetLatLng));

                    }
                }
            });
            builder.setNegativeButton("Cancel", null);

            builder.show();
        }else{
            Toast.makeText(MapsOverlayActivity.this, "Map not ready", Toast.LENGTH_LONG).show();
        }
    }





}




