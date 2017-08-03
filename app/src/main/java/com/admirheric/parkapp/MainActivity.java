package com.admirheric.parkapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parkbob.ParkbobManager;
import com.parkbob.models.GeoSpace;
import com.parkbob.models.Geometry;
import com.parkbob.models.PointCoordinate;
import com.parkbob.models.RulesContext;
import com.parkbob.models.TrafficRule;

import java.util.List;

import static com.admirheric.parkapp.R.id.map;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final String CURRENT_POSITION = "Current position";
    private static final double VIENNA_LAT = 48.2202598;
    private static final double VIENNA_LON = 16.3721741;
    private static final float DEFAULT_ZOOM = 18.0f;

    private GoogleMap mMap;
    private List<GeoSpace> rules;

    private Handler handler;

    private TextView tvNow;
    private TextView tvLater;
    private TextView tvInfo;
    private ProgressBar pbInfo;
    private FloatingActionButton fabNavigate;
    private LinearLayout llNowLaterButtons;
    private LinearLayout llInfoContent;
    private ScrollView svInfo;

    private double destinationLat;
    private double destinationLon;

    LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvNow = (TextView) findViewById(R.id.tvNow);
        tvLater = (TextView) findViewById(R.id.tvLater);
        tvInfo = (TextView) findViewById(R.id.tvInfo);
        pbInfo = (ProgressBar) findViewById(R.id.pbInfo);
        fabNavigate = (FloatingActionButton) findViewById(R.id.fabNavigate);
        llNowLaterButtons = (LinearLayout) findViewById(R.id.llNowLaterButtons);
        llInfoContent = (LinearLayout) findViewById(R.id.llInfoContent);
        svInfo = (ScrollView) findViewById(R.id.svInfo);

        tvNow.setOnClickListener(this);
        tvLater.setOnClickListener(this);
        fabNavigate.setOnClickListener(this);

        inflater = LayoutInflater.from(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        ParkbobManager.getInstance().bindService(this);

        ParkbobManager.getInstance().activateRecognitionEngine();

        //if first run start splash activity
        if(MyApplication.getInstance().isFirstRun()){
            startSplashActivity();
        }

        handler = new Handler();

    }

    private void startSplashActivity(){
        Intent i = new Intent(getApplicationContext(), SplashActivity.class);
        startActivity(i);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //default destination and map center
        destinationLat = VIENNA_LAT;
        destinationLon = VIENNA_LON;

        // Add a marker in Vienna and move the camera
        final LatLng vienna = new LatLng(VIENNA_LAT, VIENNA_LON);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vienna, DEFAULT_ZOOM));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                updateNowLaterButtons(true);
                tvInfo.setVisibility(View.GONE);
                svInfo.setVisibility(View.GONE);
                pbInfo.setVisibility(View.VISIBLE);

                //clear map
                mMap.clear();
                //add new marker
                mMap.addMarker(new MarkerOptions().position(latLng).title(CURRENT_POSITION));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                destinationLat = latLng.latitude;
                destinationLon = latLng.longitude;

                loadRules(latLng);
            }
        });
    }

    private void loadRules(final LatLng latLng){
        new Thread(new Runnable() {
            @Override
            public void run() {
                RulesContext rulesContext = ParkbobManager.getInstance().getRulesContext(latLng);
                if(rulesContext!=null){
                    rules = rulesContext.getGeoSpaceList();

                    //draw parking rules on map
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            showRulesOnMap(true);
                        }
                    });
                } else {
                    Utils.log("rules null");
                }
            }
        }).start();
    }

    private void showRulesOnMap(boolean isCurrent) {

        //show now/later buttons and hide progress bar
        llInfoContent.removeAllViews();
        svInfo.setVisibility(View.VISIBLE);
        pbInfo.setVisibility(View.GONE);
        llNowLaterButtons.setVisibility(View.VISIBLE);

        if(rules!=null){
            for(int i=0;i<rules.size();i++){
                final TrafficRule rule;
                if(isCurrent){
                    rule = rules.get(i).getCurrentTrafficRule();
                } else {
                    rule = rules.get(i).getFutureTrafficeRule();
                }

                if(rule!=null){
                    Utils.log("rule name " + rule.getRuleName());

                    final List<Geometry> geometries = rules.get(i).getGeometries();
                    //draw all geometries
                    for(int j=0;j<geometries.size();j++){
                        final Geometry geometry = geometries.get(j);
                        if(geometry!=null){
                            showRuleInfo(rule);

                            if(geometry.getType().equals(Geometry.GeometryType.POINT)){
                                //draw point
                                PointCoordinate p = (PointCoordinate) geometry;
                                p.drawOnMap(mMap, getRuleColor(rule), rule.getPointRestrictionType());
                            } else {
                                //draw line
                                geometry.drawOnMap(mMap, getRuleColor(rule));
                            }
                            geometry.setActivated(true, getApplicationContext());
                        }
                    }
                }
            }
        }
    }

    //add element to info box scroll view
    private void showRuleInfo(TrafficRule rule) {
        View v = inflater.inflate(R.layout.park_info_item, null, false);

        String desc = rule.getRuleName();
        if(rule.getStartTime()!=null && rule.getEndTime()!=null){
            desc = desc + String.format(getString(R.string.from_to),Utils.getHoursMinutes(rule.getStartTime()),Utils.getHoursMinutes(rule.getEndTime()));
        }

        TextView description = (TextView) v.findViewById(R.id.tvDescription);
        description.setText(desc);

        TextView icon = (TextView) v.findViewById(R.id.tvIcon);
        icon.setBackgroundColor(getRuleColor(rule));

        llInfoContent.addView(v);
    }

    private void updateNowLaterButtons(boolean isCurrent) {
        if(isCurrent){
            tvNow.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
            tvLater.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
        } else {
            tvNow.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
            tvLater.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        }
    }

    public int getRuleColor(TrafficRule rule) {
        //based on rule type, return specific color
        int color;
        switch(rule.getType().ordinal()) {
            case 1:
                if(rule.getParkingCost() != null && !rule.getParkingCost().isEmpty()) {
                    color = ContextCompat.getColor(getApplicationContext(), R.color.orange);
                } else {
                    color = ContextCompat.getColor(getApplicationContext(), R.color.green);
                }
                break;
            case 2:
                color = ContextCompat.getColor(getApplicationContext(), R.color.blue);
                break;
            case 3:
                color = ContextCompat.getColor(getApplicationContext(), R.color.green);
                break;
            case 4:
                color = ContextCompat.getColor(getApplicationContext(), android.R.color.black);
                break;
            default:
                color = ContextCompat.getColor(getApplicationContext(), android.R.color.white);
        }
        return color;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tvNow:
                updateNowLaterButtons(true);
                showRulesOnMap(true);
                break;
            case R.id.tvLater:
                updateNowLaterButtons(false);
                showRulesOnMap(false);
                break;
            case R.id.fabNavigate:
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?daddr="+destinationLat+","+destinationLon));
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
