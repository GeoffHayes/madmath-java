/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage target tracking structures and algorithms.
 */
package com.madmath.tracking;

import java.io.DataOutputStream;
import java.io.IOException;

import com.madmath.math.Matrix;
import com.madmath.tracking.Observation;
import com.madmath.utilities.MessageLog;

/**
 * Abstract class to encapsulate common attributes (data members) for tracks that
 * are generally produced (predicted and updated) via any filter (typically Kalman).
 *
 */
public abstract class Track 
{
    /**
     * Default class constructor.
     */
    public Track()
    {
        MessageLog.GetInstance(_msgLog);
        
        _x                  = new Matrix();
        _P                  = new Matrix();
        _predictFilter      = null;
        _updateFilter       = null;
        _initTimeSecs       = 0.0;
        _lastUpdateTimeSecs = 0.0;
        _receivedUpdate     = false;
        _id                 = NEXT_TRACK_ID++;
    }
    
    /**
     * Class copy constructor.
     */
    public Track(final Track copy)
    {
        MessageLog.GetInstance(_msgLog);
        
        try
        {
            _x                  = new Matrix(copy._x);
            _P                  = new Matrix(copy._P);
            _predictFilter      = copy._predictFilter;
            _updateFilter       = copy._updateFilter;
            _initTimeSecs       = copy._initTimeSecs;
            _lastUpdateTimeSecs = copy._lastUpdateTimeSecs;
            _receivedUpdate     = copy._receivedUpdate;
            _id                 = copy._id;
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"Track exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    /**
     * Initializes the track with an observation.
     * 
     * @param   obs   The observation used to update the track.
     */
    public abstract void init(final Observation obs);
    
    /**
     * Predicts the track state vector and uncertainty matrix (typically forward)
     * to the input prediction time.
     * 
     * @param   predictTimeSecs   The time in seconds to predict the track to.
     */
    public abstract void predict(final double predictTimeSecs);
    
    /**
     * Updates the track state vector and uncertainty matrix given the input
     * measurement.
     * 
     * @param   obs   The measurement to update the track with.
     */
    public abstract void update(final Observation obs);
    
    /**
     * Writes the track to a data stream.
     * 
     * @param   stream   The stream to write the data to.
     * 
     * @throws IOException 
     */
    public void write(DataOutputStream stream) throws IOException
    {
        stream.writeInt(_id);
        stream.writeDouble(_initTimeSecs);
        stream.writeDouble(_lastUpdateTimeSecs);
        stream.writeBoolean(_receivedUpdate);
        _x.write(stream);
        _P.write(stream);
    }
    
    /**
     * Returns the track state vector.
     */
    public final Matrix getX()
    {
        return _x;
    }
    
    /**
     * Returns the track covaraiance matrix.
     */
    public final Matrix getP()
    {
        return _P;
    }
    
    /**
     * Returns a reference to the prediction filter.
     */
    public final PredictFilter getPredictFilter()
    {
        return _predictFilter;
    }
    
    /**
     * Returns a reference to the update filter.
     */
    public final UpdateFilter getUpdateFilter()
    {
        return _updateFilter;
    }
    
    /**
     * Returns the last update time of the track (in seconds).
     */
    public final double getLastUpdateTime()
    {
        return _lastUpdateTimeSecs;
    }
    
    /**
     * Returns the initial (creation) time of the track (in seconds).
     */
    public final double getInitTime()
    {
        return _initTimeSecs;
    }
    
    /**
     * Returns the track id.
     */
    public int getTrackId()
    {
        return _id;
    }
    
    /**
     * Returns the indicator as to whether the track has been updated with an
     * observation (or not).
     */
    public boolean receivedUpdate()
    {
        return _receivedUpdate;
    }
 
    //! 2D position and velocity state vector (x,y,vx,vy) relative to the origin.
    protected Matrix _x;
    
    //! 2D position and velocity uncertainty matrix.
    protected Matrix _P;

    //! The time (in seconds) that the track was initialized..
    protected double _initTimeSecs;
    
    //! The time (in seconds) that the track was last updated.
    protected double _lastUpdateTimeSecs;
    
    //! A utility to write messages to a log.
    protected MessageLog _msgLog;
    
    //! Track prediction filter.
    protected PredictFilter _predictFilter;
    
    //! Track update filter.
    protected UpdateFilter _updateFilter;
    
    //! Unique track identifier.
    protected int _id;
    
    //! Indicates if the track was updated with an observation.
    protected boolean _receivedUpdate;
    
    //! Class track identifier.
    private static int NEXT_TRACK_ID = 1;
    
    //! Tag string identifying class, used for logging purposes.
    private static final String TAG = "Track";
}
