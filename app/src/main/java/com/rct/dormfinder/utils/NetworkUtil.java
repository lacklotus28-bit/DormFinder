package com.rct.dormfinder.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

public class NetworkUtil {
    
    private Context context;
    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    
    public NetworkUtil(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    // Check if network is available
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    // Register network callback
    public void registerNetworkCallback(OnNetworkStateChangeListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            
            networkCallback = new NetworkCallback(listener);
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }
    
    // Unregister network callback
    public void unregisterNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // Network Callback class
    private static class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private OnNetworkStateChangeListener listener;
        
        public NetworkCallback(OnNetworkStateChangeListener listener) {
            this.listener = listener;
        }
        
        @Override
        public void onAvailable(Network network) {
            if (listener != null) {
                listener.onNetworkAvailable();
            }
        }
        
        @Override
        public void onLost(Network network) {
            if (listener != null) {
                listener.onNetworkLost();
            }
        }
    }
    
    // Interface for network state change
    public interface OnNetworkStateChangeListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }
}
