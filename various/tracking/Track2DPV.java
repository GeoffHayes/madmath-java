/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage target tracking structures (matrices) and algorithms.
 */
package com.madmath.tracking;

import java.io.DataOutputStream;
import java.io.IOException;

import com.madmath.geocoordinates.Position;
import com.madmath.geocoordinates.Vincenty;
import com.madmath.geocoordinates.Vincenty.GetRangeAzimuthData;
import com.madmath.utilities.MessageLog;

/**
 * Class encapsulating a 2D (Cartesian (x,y)) Position and Velocity track.  An
 * instance of this class is typically instantiated from a single observation,
 * and the track is then periodically or aperiodically updated with future
 * observations.
 */
public class Track2DPV extends Track
{
    //! Default constructor.
    /**
     * Class constructor.
     * 
     * @param   predictFilter         The track prediction filter.
     * @param   updateFilter          The track update/correction filter.
     * @param   veloAccuracyMtrsSec   The initial track velocity accuracy (metres 
     *                                per second).
     */
    public Track2DPV(
            PredictFilter predictFilter, UpdateFilter updateFilter, 
            final double veloAccuracyMtrsSec)
    {
        super();
        
        _predictFilter       = predictFilter;
        _updateFilter        = updateFilter;
        _origin              = new Position();
        _position            = new Position();
        _veloAccuracyMtrsSec = veloAccuracyMtrsSec;
        _vincLatLonData      = new Vincenty.GetLatLonData();
        _vincRangeAziData    = new Vincenty.GetRangeAzimuthData();
        
        if (_veloAccuracyMtrsSec<=0.0)
        {
            _veloAccuracyMtrsSec = DEF_VELO_ACCURACY_MTRS_SEC;
        }
        
        try
        {
            _x.resize(DIMS);
            _P.resize(DIMS,DIMS);
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"Track2DPV exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    /**
     * Copy constructor.
     * 
     * @param   copy   The track to copy.
     */
    public Track2DPV(final Track2DPV copy)
    { 
        super(copy);
        
        try
        {
            _position            = new Position(copy._position);
            _origin              = new Position(copy._origin);
            _veloAccuracyMtrsSec = copy._veloAccuracyMtrsSec;
            
            // no need to copy since helper only
            _vincLatLonData      = new Vincenty.GetLatLonData(); 
            _vincRangeAziData    = new Vincenty.GetRangeAzimuthData();
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"Track2DPV exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    @Override
    public void init(final Observation obs)
    {
        try
        {
            obs.init(this, _x, _P);
            
            _initTimeSecs       = obs._initTimeSecs;
            _lastUpdateTimeSecs = _initTimeSecs;
            
            _P.setAt(VX,VX,_veloAccuracyMtrsSec*_veloAccuracyMtrsSec);
            _P.setAt(VY,VY,_veloAccuracyMtrsSec*_veloAccuracyMtrsSec);
            
            _receivedUpdate = true;
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"init exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    @Override
    public void predict(final double predictTimeSecs)
    {
        final double elapsedTimeSecs = predictTimeSecs - _lastUpdateTimeSecs;
        
        if (_predictFilter != null)
        {
            try
            {
                _predictFilter.predict(this, elapsedTimeSecs);
                _receivedUpdate     = false;
                _lastUpdateTimeSecs = predictTimeSecs;
                findPosition();
            }
            catch(final Exception e)
            {
                if(_msgLog != null)
                {
                    _msgLog.writeMessage(TAG,"predict: " + e.toString(),
                                         MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
                } 
            }
        }
        else
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"predict: " + "_predictFilter not set",
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    @Override
    public void update(final Observation obs)
    {
        try
        {
            _updateFilter.update(this,  obs);
            _receivedUpdate = true;
            findPosition();
        }
        catch(final Exception e)
        {
            if (_msgLog != null)
            {
                _msgLog.writeMessage(TAG, "update: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
     
    /**
     * Finds the position using the origin.
     */
    public void findPosition()
    {
        try
        {
            final double xPos = _x.at(XPOS);
            final double yPos = _x.at(YPOS);
            
            final double rangeMtrs   = Math.sqrt(xPos*xPos + yPos*yPos);
            
            final double azimuthRads = Math.atan2(xPos,yPos);

            Vincenty.GetLatLon(_origin, rangeMtrs, azimuthRads, _vincLatLonData);

            _position.copy(_vincLatLonData._position);
        }
        catch(final Exception e)
        {
            // deliberately left blank
        }
    }
    
    /**
     * Re-orient the track state and covariance given the new reference
     * position.
     * 
     * @param   ref   New reference position.
     */
    public void reorient(final Position ref)
    {
        try
        {
            final boolean success = 
                    Vincenty.GetRangeAzimuth(ref, _position, _vincRangeAziData);
            
            if(success)
            {
                final double xPos = 
                        _vincRangeAziData._rangeMtrs*Math.sin(
                                _vincRangeAziData._fwdAzimuthRads);
                final double yPos =
                        _vincRangeAziData._rangeMtrs*Math.sin(
                                _vincRangeAziData._fwdAzimuthRads);
                
                _x.setAt(XPOS,xPos);
                _x.setAt(YPOS,yPos);
                
                // unclear if something similar needs to be done for the velocities
                // covariance????
                
                _origin.copy(ref);
            }
            else
            {
                // leave the position as is
            }
        }
        catch(final Exception e)
        {
            // deliberately left blank
        }
    }
    
    /**
     * Sets the reference origin that the track state vector is relative to.
     * 
     * @param   ref   The reference position.
     */
    public void setOrigin(final Position ref)
    {
        _origin = ref;
        findPosition();
    }
    
    /**
     * Returns the track position latitude and longitude.
     */
    public final Position getPosition()
    {
        return _position;
    }
    
    /**
     * Returns the track speed in their defined units.
     */
    public final double getSpeed()
    {
        double speed = 0.0;
        
        try
        {
            final double vx = _x.at(VX);
            final double vy = _x.at(VY);
        
            speed =  Math.sqrt(vx*vx + vy*vy);
        }
        catch(final Exception e)
        {
            // deliberately left blank
        }
        return speed;
    }
    
    /**
     * Returns the track heading in radians relative to north.
     */
    public final double getHeadingRads()
    {
        double heading = 0.0;
        
        try
        {
            final double vx = _x.at(VX);
            final double vy = _x.at(VY);
        
            heading =  Math.atan2(vx,vy);
        }
        catch(final Exception e)
        {
            // deliberately left blank
        }
        return heading;
    }
   
    
    @Override
    public void write(DataOutputStream stream) throws IOException
    {
        super.write(stream);
        
        // write the origin
        _origin.write(stream);
        _position.write(stream);
    }
   
    //! The origin latitude and longitude that the 2D position state is relative to.
    private Position _origin;
    
    //! Geographic latitude and longitude of the current position of the track.
    private Position _position;
    
    //! The track velocity accuracy (metres per second).
    private double _veloAccuracyMtrsSec;
    
    //! Vincenty data object for a new position relative to another.
    private Vincenty.GetLatLonData _vincLatLonData;
    
    //! Vincenty data object for range and azimuth.
    private Vincenty.GetRangeAzimuthData _vincRangeAziData;

    //! Indicates the number of dimensions in a 2DPV state vector and uncertainty matrix.
    public static final int DIMS = 4;
    
    //! Indicates the position of the x-coordinate in the track state and covariance "order".
    public static final int XPOS = 1;
    
    //! Indicates the position of the y-coordinate in the track state and covariance "order".
    public static final int YPOS = 2;
    
    //! Indicates the position of the velocity in x in the track state and covariance "order".
    public static final int VX   = 3;
    
    //! Indicates the position of the velocity in y in the track state and covariance "order".
    public static final int VY   = 4;
 
    //! Tag string identifying class, used for logging purposes.
    private static final String TAG = "Track2DPV";
    
    //! Default velocity uncertainty (metres per second).
    private static final double DEF_VELO_ACCURACY_MTRS_SEC = 1.5;
}
