/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage the SelfTrackerApp activity and supporting classes.
 */
package com.madmath.selftrackerv2app;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.madmath.geocoordinates.Position;
import com.madmath.geocoordinates.Vincenty;
import com.madmath.math.Matrix;
import com.madmath.tracking.Observation2DP;
 
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.Math;

/**
 * Class that implements a runnable i.e. provides an interface to a "run"
 * method to be invoked periodically, capturing the current position of 
 * the user.  An observation object is created and then used to update
 * the user's route.
 *
 */
public class ObservationGenerationRunnable implements Runnable {

    //! enumerated type representing the different supported position providers
    public enum POS_PROVIDER
    {
        NETWORK, /**< Enumeration indicating the network position data provider. */
        GPS      /**< Enumeration indicating the GPS position data provider.  */
    };
    
    //! enumerated type representing the different supported modes
    public enum SUPPORTED_MODE
    {
        REAL_DEMO, /**< Enumeration indicating the mode is real-time demo. */
        FTR_DEMO,  /**< Enumeration indicating the mode is faster-than-real demo. */
        REAL_LIVE  /**< Enumeration indicating the mode is real-time "live". */
    };
    
    
    /**
     * Class constructor that defines a listener to capture changes to the
     * current location of the user.
     * 
     * @param   context        Provides context from the parent activity so that
     *                         the location listener can be instantiated.
     * @param   appConfigData  The application config data as read in from an 
     *                         xml file.
     * @param   handler        Handler from the parent activity used to send
     *                         update route messages to the parent activity (and
     *                         so to the GUI thread).
     */
    public ObservationGenerationRunnable(
            Context context, final SelfTrackerV2Activity.AppConfigData appConfigData, Handler handler)
    {
        _obsPos                  = new Position();
        _handler                 = handler;
        _inDemoMode              = false;
        _obs2DP                  = new Observation2DP();
        _inStream                = null;
        _posDataProvider         = POS_PROVIDER.NETWORK;
        _mode                    = SUPPORTED_MODE.REAL_LIVE;
        _lastKnownLocation       = null;
        _isLocationUpdateWaiting = false;
        _updateRateMSec          = appConfigData._updateRateMSec;
        _obsPosnAccuracyMtrs     = appConfigData._obsPosnAccuracyMtrs;
        _useLocPosnAccuracy      = appConfigData._useLocPosnAccuracy;
        _obsPacket               = new ObservationPacket();
        _vincRangeAziData        = new Vincenty.GetRangeAzimuthData();
        _demofilename            = new String("");
        
        if (_obsPosnAccuracyMtrs <= 0.0)
        {
            _obsPosnAccuracyMtrs = DEFAULT_OBS_POSN_ACCURACY;
        }
        
        // set up a location listener
        if (context != null)
        {
            // Acquire a reference to the system Location Manager
            _locationManager = 
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            // Define a listener that responds to location updates
            _locationListener = new LocationListener() 
            {      
                @Override
                public void onLocationChanged(Location location) 
                {
                    _lastKnownLocation = location;
                    _isLocationUpdateWaiting = true;
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) 
                {
                    // deliberately left blank
                }

                @Override
                public void onProviderEnabled(String provider) 
                {
                    // deliberately left blank
                }

                @Override
                public void onProviderDisabled(String provider) 
                {
                    // deliberately left blank
                }
              };
              
              // ensure that the minimum time is positive
              if (_updateRateMSec <= 0)
              {
                  // default to thirty seconds
                  _updateRateMSec = DEFAULT_UPDATE_RATE_MSEC;
              }
              
              _locationManager.requestLocationUpdates(
                      ToString(_posDataProvider), _updateRateMSec, 0, _locationListener);
                
              android.util.Log.v(
                        TAG,
                        "startUp - location manager has requested location updates " +
                        "provider " + _posDataProvider);
        }
    }
    
    /**
     * Sets the provider for position data.  If there has been a change, then
     * the location manager requests new location updates from this provider.
     * 
     * @param   provider   The new position data provider.
     */
    public void setProvider(final POS_PROVIDER provider)
    {
        switch(provider)
        {
            case GPS:
            {
                if (provider != _posDataProvider)
                {
                    // switch the providers
                    if (_locationManager != null && _locationListener != null)
                    {
                        android.util.Log.v(TAG,"Switching to provider: " + 
                                LocationManager.GPS_PROVIDER);
                        _locationManager.removeUpdates(_locationListener);
                        
                        _posDataProvider = provider;
                    }
                }
                break;
            }
            case NETWORK:
            {
                if (provider != _posDataProvider)
                {
                    // switch the providers
                    if (_locationManager != null && _locationListener != null)
                    {
                        android.util.Log.v(TAG,"Switching to provider: " + 
                                LocationManager.NETWORK_PROVIDER);
                        
                        _locationManager.removeUpdates(_locationListener);
                        
                        _posDataProvider = provider;
                    }
                }
                break;
            }
        }
    }
    
    /**
     * Gets the position data provider.
     */
    public POS_PROVIDER getProvider()
    {
        return _posDataProvider;
    }
    
    /**
     * When an object implementing interface Runnable is used to create a thread, 
     * starting the thread causes the object's run method to be called in that 
     * separately executing thread.
     */
    @Override
    public void run()
    {
        final long currentTimeMSecs   = System.currentTimeMillis();
        
        // initialize the observation with the current time stamp which will
        // flag the observation as time only
        _obs2DP.init(currentTimeMSecs/SEC2MSEC);
        
        boolean obsDataExists  = false;
        
        if ((_lastKnownLocation != null && _isLocationUpdateWaiting) || _inDemoMode)
        {
            // be optimistic...
            obsDataExists = true;
            
            // has the origin been defined?
            if (_origin == null)
            { 
                _origin = new Position();
                
                if (_inDemoMode)
                {
                    if (_inStream != null)
                    {
                        obsDataExists = readObsFromFile(_obs2DP);
                        _origin.copy(_obs2DP.getOrigin());
                    }
                }
                else
                {
                    _origin.setLatitude(_lastKnownLocation.getLatitude(), 
                            Position.POS_VALUE_TYPE.DEGREES);
                    _origin.setLongitude(_lastKnownLocation.getLongitude(),
                            Position.POS_VALUE_TYPE.DEGREES);    
                    _isLocationUpdateWaiting = false;
                }
            }
            
            // get the latitude and longitude from last known location
            if (_inDemoMode)
            {
                if (_inStream != null)
                {
                    obsDataExists = readObsFromFile(_obs2DP); 
                }
            }
            else
            {
                _obsPos.setLatitude(_lastKnownLocation.getLatitude(), 
                        Position.POS_VALUE_TYPE.DEGREES);
                _obsPos.setLongitude(_lastKnownLocation.getLongitude(),
                        Position.POS_VALUE_TYPE.DEGREES);  
                _isLocationUpdateWaiting = false;
            }
            
            if (obsDataExists && !_inDemoMode)
            {        
                // compute range and azimuth of the new position relative to the
                // origin
                try
                {
                    Vincenty.GetRangeAzimuth(_origin, _obsPos, _vincRangeAziData);
    
                    // determine the (x,y) coordinate
                    final double x = _vincRangeAziData._rangeMtrs*
                            Math.sin(_vincRangeAziData._fwdAzimuthRads);
                    final double y = _vincRangeAziData._rangeMtrs*
                            Math.cos(_vincRangeAziData._fwdAzimuthRads);
                    
                    // create the observation
                    _obs2DP.setInitTime(currentTimeMSecs/SEC2MSEC);
                    
                    _obs2DP.set(_origin);
                    
                    Matrix z = _obs2DP.getZ();
                    Matrix R = _obs2DP.getR();
                    
                    z.setAt(Observation2DP.XPOS,x);
                    z.setAt(Observation2DP.YPOS,y);
                    
                    double posUncertainty = 0.0;
                    
                    if (_lastKnownLocation.hasAccuracy() && _useLocPosnAccuracy)
                    {
                        posUncertainty = _lastKnownLocation.getAccuracy();
                    }
                    else
                    {
                        posUncertainty = _obsPosnAccuracyMtrs;
                    }
                    
                    R.setAt(Observation2DP.XPOS,Observation2DP.XPOS,posUncertainty);
                    R.setAt(Observation2DP.YPOS,Observation2DP.YPOS,posUncertainty);
                    
                    // z and R have been set so indicate that the observation is not
                    // time only
                    _obs2DP.setIsTimeOnly(false);
                }
                catch(final Exception e)
                {
                    android.util.Log.e(TAG,"::run - exception: " + e.toString());
                }
            }
        }
        else
        {
            // commented out for now since given the high update rate, the track can go
            // wildly off course
            
            // no new observation exists, so we should publish the time only so that the
            // track predicts forward in time
            //obsDataExists=true;
            //_obs2DP.setIsTimeOnly(true);
        }
        
        if (obsDataExists)
        {
            // initialize the packet
            _obsPacket.init(_obs2DP.getInitTime(), _obs2DP); 
    
            Message msg = Message.obtain();
            msg.obj = _obsPacket;
            _handler.sendMessage(msg);
        }
        
        // invoke itself again
        switch(_mode)
        {
            case REAL_LIVE:
            {
                _handler.postDelayed(this, _updateRateMSec);
                break;
            }
            case REAL_DEMO:
            {
                if (obsDataExists)
                {
                    _handler.postDelayed(this, _updateRateMSec);
                }
                else
                {
                    android.util.Log.v(TAG,"run - ending real-time demo observation invocation");
                }
                break;
            }
            case FTR_DEMO:
            {
                if (obsDataExists)
                {
                    _handler.postDelayed(this, FTR_UPDATE_RATE_MSEC);
                }
                else
                {
                    android.util.Log.v(TAG,"run - ending FTR demo observation invocation");
                }
            }
        }
    }
    
    /**
     * Toggles the mode of the runnable.  If the toggled mode is the same as
     * the current mode, then the mode is set to REAL_LIVE.
     * 
     * @return  The current mode.
     */
    public SUPPORTED_MODE toggleMode(final SUPPORTED_MODE mode)
    {
        if (mode==_mode)
        {
            _mode = SUPPORTED_MODE.REAL_LIVE;
            _inDemoMode = false;
        }
        else
        {
            _mode = mode;
            _inDemoMode = (_mode != SUPPORTED_MODE.REAL_LIVE);
        }
        
        if (!_inDemoMode && _inStream != null)
        {
            try
            {
                _inStream.close();
                _inStream = null;
            }
            catch (final Exception e)
            {
                android.util.Log.e(TAG,"toggleDemoMode exception: " + e.toString());
            }
            finally
            {
                _inStream = null;
            }
        }

        return _mode;
    }
    
    /**
     * Indicates whether the demo mode is enabled or disabled.
     * 
     * @retval   true if the demo mode is enabled
     * @retval   false if the demo mode is disabled
     */
    public boolean inDemoMode()
    {
        return _inDemoMode;
    }
    
    /**
     * Returns the current mode of the runnable.
     */
    public SUPPORTED_MODE getMode()
    {
        return _mode;
    }
    
    /**
     * Sets the demo filename (the path is assumed known).
     * 
     * @param   filename   The filename of the demo file.
     */
    public void setDemoFile(final String filename)
    {
        _demofilename = filename;
    }
    
    /**
     * Closes any open resources/files due to a transition of the parent
     * activity to a stop state.
     */
    public void shutDown()
    {
        if (_inStream != null)
        {
            try
            {
                _inStream.close();
                _inStream = null;
            }
            catch(final Exception e)
            {
                android.util.Log.e(TAG,"shutDown exception: " + e.toString());
            }
        }
        
        if (_locationManager != null && _locationListener != null)
        {
            _locationManager.removeUpdates(_locationListener);
        }
    }
    
    /**
     * Resets the instance to a default state.  Opens the demo data file if in
     * demo mode, else registers the location listener for the appropriate provider.
     * 
     * @warning   If the runnable is in demo mode, this mode remains unchanged.
     */
    public void startUp()
    {
        _origin = null;
        
        try
        {
            if (_inDemoMode)
            {
                try
                {
                    if (_inStream != null)
                    {
                        _inStream.close();
                    }
                    _inStream = null;

                    if (_demofilename.isEmpty())
                    {
                        _demofilename = DATA_FILE;
                    }

                    File path        = Environment.getExternalStoragePublicDirectory(DATA_DIR);
                    File pathAndFile = new File(path,_demofilename);
                    _inStream        = new DataInputStream(new FileInputStream(pathAndFile));
                    android.util.Log.v(TAG,"startUp - demo data file has been opened");
                }
                catch(final Exception e)
                {
                    android.util.Log.e(TAG,"startUp exception: " + e.toString());
                }
            }
            else
            {
                // register the listener with the Location Manager to receive location updates
                _locationManager.requestLocationUpdates(
                      ToString(_posDataProvider), _updateRateMSec, 0, _locationListener);
                
                android.util.Log.v(
                        TAG,
                        "startUp - location manager has requested location updates " +
                        "provider " + _posDataProvider);
            }
            
        }
        catch(final Exception e)
        {
            android.util.Log.e(TAG,"startUp exception: " + e.toString());
        }
    }
    
    /**
     * Reads the observation data from file and saves it to the passed argument.  
     * 
     * @param   obs   The observation to be updated with that from file.
     * 
     * @retval  true if the observation data has been successfully read from file.
     * @retval  false if the observation data has not been successfully read from file (eof).
     */
    private boolean readObsFromFile(Observation2DP obs)
    {
        boolean status = false;
        
        if (_inStream != null)
        {
            try
            {
                obs.read(_inStream);
                status = true;
            }
            catch(final Exception e)
            {
                android.util.Log.e(TAG,"readObsFromFile exception: " + e.toString());
            }
        }
        
        return status;
    }
    
    /**
     * Converts the position data provider enum type to the equivalent 
     * LocationManager provider string representation.
     * 
     * @param   providerType   The provider type to convert to a string.
     */
    public static String ToString(final POS_PROVIDER providerType)
    {
        String providerStr = new String("");
        
        switch(providerType)
        {
            case GPS:
            {
                providerStr = LocationManager.GPS_PROVIDER;
                break;
            }
            case NETWORK:
            {
                providerStr = LocationManager.NETWORK_PROVIDER;
                break;
            }
        }
        return providerStr;
    }
    
    //! Last known location
    private Location _lastKnownLocation;
    
    //! Flag indicating that there is a location update waiting.
    private boolean _isLocationUpdateWaiting;
    
    //! Tag string identifying class, used for logging purposes.
    private static final String TAG = "GenerateObservationRunnable";
    
    //! Directory to read self-tracker data files from.
    private static final String DATA_DIR = "Apps/SelfTrackerV2/obsdat";
    
    //! The faster-than-real time update rate in milliseconds.
    private static final long FTR_UPDATE_RATE_MSEC = 250;
    
    //! Default demo file name.
    private static final String DATA_FILE = "demo.dat";
    
    //! Reference to handler from parent thread.
    private Handler _handler;
    
    //! The observation packet to publish to the activity "thread".
    private ObservationPacket _obsPacket;
    
    //! The data object used in the Vincenty get range and azimuth call.
    Vincenty.GetRangeAzimuthData _vincRangeAziData;
    
    //! Update rate of runnable via the handler.
    private long _updateRateMSec;
    
    //! Indicator as to whether to use the Location position accuracy or not for an observation.
    private boolean _useLocPosnAccuracy;
    
    //! The observation position accuracy (metres) if _useLocaPosnAccuracy is false or in demo mode.
    private double _obsPosnAccuracyMtrs;
    
    //! The origin of the coordinate system.
    private Position _origin;
    
    //! The current observation position.
    private Position _obsPos;
    
    //! Flag indicating whether demo data should be used instead of true position data.
    private boolean _inDemoMode;
 
    //! Observation object used for passing data from the runnable to the caller/activity.
    private Observation2DP _obs2DP;
    
    //! Input stream object to read demo data from file.
    private DataInputStream _inStream;

    //! The default update rate in milliseconds.
    private final static long DEFAULT_UPDATE_RATE_MSEC = 5000;
    
    //! The default observation position accuracy (metres).
    private final static double DEFAULT_OBS_POSN_ACCURACY = 50.0;
    
    //! Converts seconds to milliseconds.
    private final static double SEC2MSEC = 1000.0;
    
    //! The provider to be used for position information (one of GPS or NETWORK).
    private POS_PROVIDER _posDataProvider;
    
    //! The mode that the runnable is in.
    private SUPPORTED_MODE _mode;
    
    //! A reference to the location listener.
    private LocationListener _locationListener;
    
    //! A reference to the location manager.
    private LocationManager _locationManager;
    
    //! The filename (no path) of the demo to run.
    private String _demofilename;
}
