package org.nsdev.apps.superhappyhackmap;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.*;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service class that starts a listener for GPS location, which should keep the
 * GPS lock indefinitely while running.
 * <p/>
 * Created by neal 13-03-03 1:42 PM
 */
public class LocationLockService extends Service
{
    public static final String ACTION_LOCKGPS = "org.nsdev.apps.superhappyhackmap.lockgps";
    public static final String ACTION_FENCEUPDATE = "org.nsdev.apps.superhappyhackmap.fenceupdate";
    public static final String ACTION_HACK = "org.nsdev.apps.ingresstollbelt.hack";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private LocationClient locationClient;

    private static Location currentLocation;

    public static class Hack
    {

        private Date date;
        private Location location;

        public Hack(Date date, Location location)
        {
            this.date = date;
            this.location = location;
        }

        public Date getDate()
        {
            return date;
        }

        public void setDate(Date date)
        {
            this.date = date;
        }

        public Location getLocation()
        {
            return location;
        }

        public void setLocation(Location location)
        {
            this.location = location;
        }
    }

    private static ArrayList<Hack> hacks = new ArrayList<Hack>(15);

    public static void addHack(Date date, Location location)
    {
        hacks.add(new Hack(date, location));
    }

    public static ArrayList<Hack> getHacks()
    {
        return hacks;
    }

    private LocationListener locationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            Log.d("TAG", "Got a location update: " + location.toString());
            currentLocation = location;
        }
    };

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler
    {
        public ServiceHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.arg2)
            {
                case 0:
                {
                    stopSelf(msg.arg1);
                    break;
                }
                case 1:
                {
                    if (locationClient != null)
                    {
                        return;
                    }

                    locationClient = new LocationClient(LocationLockService.this, new GooglePlayServicesClient.ConnectionCallbacks()
                    {
                        @Override
                        public void onConnected(Bundle bundle)
                        {
                            LocationRequest request = LocationRequest.create();
                            request.setPriority(LocationRequest.PRIORITY_NO_POWER);
                            request.setFastestInterval(1000);
                            request.setInterval(5000);

                            locationClient.requestLocationUpdates(request, locationListener);

                            Toast.makeText(LocationLockService.this, "GPS Locked", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDisconnected()
                        {
                            Toast.makeText(LocationLockService.this, "GPS Unlocked", Toast.LENGTH_SHORT).show();
                        }
                    }, new GooglePlayServicesClient.OnConnectionFailedListener()
                    {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult)
                        {
                            Log.e("TAG", "Connection failed.");
                        }
                    }
                    );

                    locationClient.connect();


                    /*
                    LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    criteria.setCostAllowed(false);

                    locationManager.requestLocationUpdates(1000L, 2f, criteria, mLocationListener, getLooper());
                    */

                    break;
                }
                case 2:
                {
                    Toast.makeText(LocationLockService.this, "Fence Update", Toast.LENGTH_LONG).show();

                    Intent intent = (Intent)msg.getData().get("intent");

                    if (LocationClient.hasError(intent))
                    {
                        // Get the error code with a static method
                        int errorCode = LocationClient.getErrorCode(intent);
                        // Log the error
                        Log.e("ReceiveTransitionsIntentService",
                                "Location Services error: " +
                                        Integer.toString(errorCode));
                        /*
                         * You can also send the error code to an Activity or
                         * Fragment with a broadcast Intent
                         */
                        /*
                         * If there's no error, get the transition type and the IDs
                         * of the geofence or geofences that triggered the transition
                         */
                    }
                    else
                    {
                        // Get the type of transition (entry or exit)
                        int transitionType = LocationClient.getGeofenceTransition(intent);

                        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                        {
                            Toast.makeText(LocationLockService.this, "Entered Zone", Toast.LENGTH_LONG).show();
                        }
                        else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)
                        {
                            Toast.makeText(LocationLockService.this, "Exited Zone", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            Toast.makeText(LocationLockService.this, "Unknown transition " + transitionType, Toast.LENGTH_LONG)
                                 .show();
                        }

                        // Test that a valid transition was reported
                        if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                                || (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT))
                        {
                            List<Geofence> triggerList =
                                    LocationClient.getTriggeringGeofences(intent);

                            String[] triggerIds = new String[triggerList.size()];

                            for (int i = 0; i < triggerIds.length; i++)
                            {
                                // Store the Id of each geofence
                                triggerIds[i] = triggerList.get(i).getRequestId();
                            }
                            /*
                             * At this point, you can store the IDs for further use
                             * display them, or display the details associated with
                             * them.
                             */
                        }
                        else
                        {
                            // An invalid transition was reported
                        }
                    }
                }

                case 3:
                    Toast.makeText(LocationLockService.this, "Recording Hack Location", Toast.LENGTH_SHORT).show();

                    Log.d("TAG", "Location Changed");

                    Location location = locationClient.getLastLocation();

                    Geofence.Builder builder = new Geofence.Builder();
                    builder.setCircularRegion(location.getLatitude(), location.getLongitude(), 40)
                           .setExpirationDuration(5 * 60 * 1000)
                           .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                           .setRequestId("ingress-toolbelt-0");
                    Geofence fence = builder.build();

                    List<Geofence> fences = new ArrayList<Geofence>();
                    fences.add(fence);

                    /*
                    circleOptions = new CircleOptions()
                            .center(new LatLng(location.getLatitude(), location.getLongitude()))
                            .radius(40); // In meters

                    map.addCircle(circleOptions);
                    */

                    Intent i = new Intent(getBaseContext(), LocationLockService.class);
                    i.setAction(LocationLockService.ACTION_FENCEUPDATE);

                    PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(), 0, i, 0);

                    locationClient.addGeofences(fences, pendingIntent, new LocationClient.OnAddGeofencesResultListener()
                    {
                        @Override
                        public void onAddGeofencesResult(int i, String[] strings)
                        {
                            Log.d("TAG", "Result " + i);
                        }
                    });

            }
        }
    }


    @Override
    public void onCreate()
    {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent == null)
        {
            return START_STICKY;
        }

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.arg2 = (ACTION_LOCKGPS.equals(intent.getAction())) ? 1 : 0;
        if (ACTION_FENCEUPDATE.equals(intent.getAction()))
        {
            msg.arg2 = 2;
        }
        else if (ACTION_HACK.equals(intent.getAction()))
        {
            msg.arg2 = 3;
        }

        Bundle b = new Bundle();
        b.putParcelable("intent", intent);
        msg.setData(b);
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy()
    {
        locationClient.disconnect();
    }

    static Location getCurrentLocation()
    {
        return currentLocation;
    }
}