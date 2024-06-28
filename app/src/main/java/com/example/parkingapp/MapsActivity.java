package com.example.parkingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap mMap;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    private List<ParkingSpot> parkingSpots = new ArrayList<>();
    private LatLng clickedPosition;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private TextView userNameTextView;
    private LinearLayout userInfoLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mapView = findViewById(R.id.mapView);
        ImageView avatarImageView = findViewById(R.id.avatarImageView);
        userInfoLayout = findViewById(R.id.userInfoLayout);
        userNameTextView = findViewById(R.id.userNameTextView);
        Button logoutButton = findViewById(R.id.logoutButton);
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            fetchUserName();
        }

        avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userInfoLayout.getVisibility() == View.GONE) {
                    userInfoLayout.setVisibility(View.VISIBLE);
                } else {
                    userInfoLayout.setVisibility(View.GONE);
                }
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
    }

    private void fetchUserName() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String firstName = dataSnapshot.child("firstName").getValue(String.class);
                    String lastName = dataSnapshot.child("lastName").getValue(String.class);
                    userNameTextView.setText(firstName + " " + lastName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapsActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("parkingSpots");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Clear existing markers
                mMap.clear();
                parkingSpots.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    double latitude = snapshot.child("latitude").getValue(Double.class);
                    double longitude = snapshot.child("longitude").getValue(Double.class);
                    String title = snapshot.child("title").getValue(String.class);
                    int availableSpots = snapshot.child("availableSpots").getValue(Integer.class);

                    LatLng parkingArea = new LatLng(latitude, longitude);
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(parkingArea)
                            .title(title)
                            .snippet("Available spots: " + availableSpots)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                    ParkingSpot parkingSpot = new ParkingSpot(parkingArea, availableSpots, marker);
                    parkingSpots.add(parkingSpot);

                    if (availableSpots == 0) {
                        marker.setTag("zeroSpots");
                    } else {
                        marker.setTag("availableSpots");
                    }
                }

                // Move the camera to the first parking area
                if (dataSnapshot.getChildrenCount() > 0) {
                    DataSnapshot firstSnapshot = dataSnapshot.getChildren().iterator().next();
                    double firstLat = firstSnapshot.child("latitude").getValue(Double.class);
                    double firstLng = firstSnapshot.child("longitude").getValue(Double.class);
                    LatLng firstParkingArea = new LatLng(firstLat, firstLng);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(firstParkingArea));
                }

                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        if ("zeroSpots".equals(marker.getTag())) {
                            clickedPosition = marker.getPosition();
                            notifyZeroSpots(marker.getTitle());
                        }
                        return false;
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });
    }

    private void notifyZeroSpots(String title) {
        new AlertDialog.Builder(this)
                .setTitle("No Available Spots")
                .setMessage("Parking area \"" + title + "\" has zero available spots! Now, nearest available parking area will be highlighted.")
                .setPositiveButton("OK", (dialog, which) -> highlightNearestAvailableSpot())
                .show();
    }

    private void highlightNearestAvailableSpot() {
        if (clickedPosition == null) return;

        ParkingSpot nearestSpot = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ParkingSpot spot : parkingSpots) {
            if (spot.getAvailableSpots() > 0) {
                double distance = distanceBetween(clickedPosition, spot.getPosition());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestSpot = spot;
                }
            }
        }

        if (nearestSpot != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(nearestSpot.getPosition()));
            nearestSpot.getMarker().showInfoWindow();
        }
    }

    private double distanceBetween(LatLng pos1, LatLng pos2) {
        double lat1 = pos1.latitude;
        double lng1 = pos1.longitude;
        double lat2 = pos2.latitude;
        double lng2 = pos2.longitude;

        double earthRadius = 6371; // kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private static class ParkingSpot {
        private final LatLng position;
        private final int availableSpots;
        private final Marker marker;

        public ParkingSpot(LatLng position, int availableSpots, Marker marker) {
            this.position = position;
            this.availableSpots = availableSpots;
            this.marker = marker;
        }

        public LatLng getPosition() {
            return position;
        }

        public int getAvailableSpots() {
            return availableSpots;
        }

        public Marker getMarker() {
            return marker;
        }
    }
}
