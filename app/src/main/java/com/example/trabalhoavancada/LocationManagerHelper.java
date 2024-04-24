package com.example.trabalhoavancada;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class LocationManagerHelper extends Thread {

    private static final String TAG = "LocationManagerHelper";

    private Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;

    public LocationManagerHelper(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        Looper.prepare();

        // Inicializa o LocationManager e o LocationListener
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                handleLocationChanged(location);
            }

            @Override
            public void onProviderDisabled(String provider) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        // Verifica as permissões e solicita atualizações de localização
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,  // Intervalo de atualização em milissegundos
                    10,    // Distância mínima de atualização em metros
                    locationListener
            );
        }

        Looper.loop();
    }

    private void handleLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        Log.d(TAG, "Nova localização recebida - Latitude: " + latitude + ", Longitude: " + longitude);

        // Atualiza a latitude e longitude no MapsActivity
        ((MapsActivity) context).setLatitude(latitude);
        ((MapsActivity) context).setLongitude(longitude);
        ((MapsActivity) context).atualizarMapa();
    }

    public void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}

