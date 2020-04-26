package com.dell.batterycheck;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.dell.batterycheck.databinding.ActivityMainBinding;
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
    final static String REQUESTING_LOCATION_UPDATES_KEY = getREQUESTING_LOCATION_UPDATES_KEY();
    private static long UPDATE_INTERVAL_IN_MILLISECONDS = getUPDATE_INTERVAL_IN_MILLISECONDS();
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
            mCurrentLocation = getmCurrentLocation();
            mLastUpdateTime = getmLastUpdateTime();
             updateUI();
        }
    }
        @Override
    public void startUpdatesButtonHandler(View view) {
        super.startUpdatesButtonHandler(view);
        if(global_button)
        {
            setButtonsEnabledState();
        }
    }

    @Override
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates)
        {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
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
        double lati1 = Math.toRadians(latitude1);
        double longi1 = Math.toRadians(longitude1);
        double lati2 = Math.toRadians(latitude2);
        double longi2 = Math.toRadians(longitude2);

        double earthRadius = 6371.01;
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(global_permission)
        {
            setButtonsEnabledState();
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        mCurrentLocation = getmCurrentLocation();
        mLastUpdateTime = getmLastUpdateTime();
        updateUI();
        String location_updated_message = "Location Updated" + UPDATE_INTERVAL_IN_MILLISECONDS ;
        Toast.makeText(this, getResources().getString(R.string.location_updated_message), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, location_updated_message, Toast.LENGTH_SHORT).show();
    }
}