package com.dell.batterycheck;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.dell.batterycheck.databinding.ActivityMainBinding;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends working{

    private String mLatitudeLabel;
    private String mLongitudeLabel;
    private String mLastUpdateTimeLabel;
    private ActivityMainBinding mBinding;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    Boolean mRequestingLocationUpdates;
    final static String REQUESTING_LOCATION_UPDATES_KEY = getREQUESTING_LOCATION_UPDATES_KEY();
    private final static String LOCATION_KEY = getLOCATION_KEY();
    private final static String LAST_UPDATED_TIME_STRING_KEY = getLAST_UPDATED_TIME_STRING_KEY();
    //private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = getPERMISSIONS_REQUEST_ACCESS_FINE_LOCATION();
    private static final int REQUEST_CHECK_SETTINGS = getREQUEST_CHECK_SETTINGS();
    private static long UPDATE_INTERVAL_IN_MILLISECONDS = getUPDATE_INTERVAL_IN_MILLISECONDS();
    LocationRequest mLocationRequest = getmLocationRequest();

    //mGoogleApiClient
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLastUpdateTimeLabel = getResources().getString(R.string.last_update_time_label);
        textView = findViewById(R.id.percentageLabel);
        mBinding.percentageLabel.setText(getBatteryInfo1(0));
        mBinding.running.setText(getBatteryInfo1(1));
        mBinding.timeRunning.setText(getBatteryInfo1(2));
        mCurrentLocation = getmCurrentLocation();
        mLastUpdateTime = getmLastUpdateTime();
       // buildGoogleApiClient();
        //this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mLocationRequest = getmLocationRequest();
        mRequestingLocationUpdates = getmRequestingLocationUpdates();
    }

    @Override
    public void setVal(String v1, String v2, String v3) {
        super.setVal(v1, v2, v3);
        mBinding.percentageLabel.setText(v1);
        mBinding.running.setText(v2);
        mBinding.timeRunning.setText(v3);
    }

    @Override
    public void updateValuesFromBundle(Bundle savedInstanceState) {
        super.updateValuesFromBundle(savedInstanceState);
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
             updateUI();
        }
    }



    @Override
    public void startUpdatesButtonHandler(View view) {
        clearUI();
        if (!isPlayServicesAvailable(this)) return;
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
        } else {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setButtonsEnabledState();
            startLocationUpdates();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                showRationaleDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    @Override
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    @Override
    public void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates");

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, MainActivity.this);
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });

    }

    void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mBinding.startUpdatesButton.setEnabled(false);
            mBinding.stopUpdatesButton.setEnabled(true);
        } else {
            mBinding.startUpdatesButton.setEnabled(true);
            mBinding.stopUpdatesButton.setEnabled(false);
        }
    }

    private void clearUI() {
        mBinding.latitudeText.setText("");
        mBinding.longitudeText.setText("");
        mBinding.lastUpdateTimeText.setText("");
    }
    Double latitude1;
    Double longitude1;
    String lat1;
    String long1;
    Double latitude2;
    Double longitude2;
    private void updateUI() {

        if (mCurrentLocation == null) return;
        if(mBinding.longitudeText.getText().toString().isEmpty() ||  mBinding.latitudeText.getText().toString().isEmpty())
        {
            latitude1 = mCurrentLocation.getLatitude();
            longitude1 = mCurrentLocation.getLongitude();
        }
        else
        {
            lat1 = mBinding.latitudeText.getText().toString().substring(10);
            long1 = mBinding.longitudeText.getText().toString().substring(11);
            latitude1 = Double.parseDouble(lat1);
            longitude1 = Double.parseDouble(long1);
        }
        mBinding.x1.setText(String.format("%s: %f", mLatitudeLabel,
                latitude1));
        mBinding.y1.setText(String.format("%s: %f", mLongitudeLabel,
                longitude1));
        latitude2 = mCurrentLocation.getLatitude();
        longitude2 = mCurrentLocation.getLongitude();
        double dist = 0;
        double lati1 = Math.toRadians(latitude1);
        double longi1 = Math.toRadians(longitude1);
        double lati2 = Math.toRadians(latitude2);
        double longi2 = Math.toRadians(longitude2);

        double earthRadius = 6371.01; //Kilometers
        total_distance += 1000 * earthRadius * Math.acos(Math.sin(lati1)*Math.sin(lati2) + Math.cos(lati1)*Math.cos(lati2)*Math.cos(longi1 - longi2));

        mBinding.dist.setText(Double.toString(total_distance));
        mBinding.latitudeText.setText(String.format("%s: %f", mLatitudeLabel,
                latitude2));
        mBinding.longitudeText.setText(String.format("%s: %f", mLongitudeLabel,
                longitude2));
        mBinding.x2.setText(String.format("%s: %f", mLatitudeLabel,
                latitude2));
        mBinding.y2.setText(String.format("%s: %f", mLongitudeLabel,
                longitude2));

        mBinding.lastUpdateTimeText.setText(String.format("%s: %s", mLastUpdateTimeLabel,
                mLastUpdateTime));
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
//        LocalDateTime now = LocalDateTime.now();
//        System.out.println(dtf.format(now));
        SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy");
        String date = sdf.format(new Date());
        System.out.println(date);
        String loc = String.format("%s: %f", mLatitudeLabel,
                latitude2) + String.format("%s: %f", mLongitudeLabel,
                longitude2) + " dist: " +  total_distance + "battery: " + mBinding.percentageLabel.getText().toString() + "dileep";
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference reference = db.getReference(date);
        reference.setValue(loc);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setButtonsEnabledState();
                    startLocationUpdates();
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        mRequestingLocationUpdates = false;
                        Toast.makeText(MainActivity.this, "To enable the function of this application, please enable the location information permission of the application from the setting screen of the terminal.", Toast.LENGTH_SHORT).show();
                    } else {
                        showRationaleDialog();
                    }
                }
                break;
            }
        }
    }
    @Override
    public void showRationaleDialog() {
        new AlertDialog.Builder(this)
                .setPositiveButton("To give permission", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                    }
                })
                .setNegativeButton("don't do", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this,"Location permission not allowed", Toast.LENGTH_SHORT).show();
                        mRequestingLocationUpdates = false;
                    }
                })
                .setCancelable(false)
                .setMessage("This app needs to allow the use of location information.")
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        super.onConnected(bundle);
    }
    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        updateUI();
        String location_updated_message = "Location Updated" + UPDATE_INTERVAL_IN_MILLISECONDS ;
        Toast.makeText(this, getResources().getString(R.string.location_updated_message), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, location_updated_message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onConnectionSuspended(int i) {
        super.onConnectionSuspended(i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        super.onConnectionFailed(connectionResult);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean moveTaskToBack(boolean nonRoot) {
        return super.moveTaskToBack(nonRoot);
    }






}