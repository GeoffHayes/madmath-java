/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage target tracking structures (matrices) and algorithms.
 */
package com.madmath.tracking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.madmath.math.Matrix;
import com.madmath.utilities.MessageLog;

/**
 * Abstract class that defines the common attributes of and the interfaces to
 * an observation.
 *
 */
public abstract class Observation 
{
    /**
     * Exception for invalid pairing of an observation with a track.
     */
    public class InvalidObsTrackPairException extends Exception
    {
        /**
         * Class constructor.  
         */
        InvalidObsTrackPairException()
        {
            super();
        }
        
        /**
         * Class constructor. 
         * 
         * @param   msg   String message that can be specified at time of 
         *                exception creation.
         */
        InvalidObsTrackPairException(String msg)
        {
            super(msg);
        }
    };
    
    /**
     * Default class constructor.
     */
    public Observation()
    {
        MessageLog.GetInstance(_msgLog);
        
        _initTimeSecs = 0.0;
        _isTimeOnly   = false;
        _z            = new Matrix();
        _R            = new Matrix();
        _id           = NEXT_OBS_ID++;
    }
    
    /**
     * Class copy constructor.
     */
    public Observation(final Observation copy)
    {
        MessageLog.GetInstance(_msgLog);
        
        try
        {
            _z                  = new Matrix(copy._z);
            _R                  = new Matrix(copy._R);
            _initTimeSecs       = copy._initTimeSecs;
            _id                 = copy._id;
            _isTimeOnly         = copy._isTimeOnly;
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"Observation exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    /**
     * Returns the H matrix which maps the true state space of the model into the
     * observed space.
     * 
     * @param   track   The 2DPV track to indicate to the observation sub-class
     *                  what the mapping matrix should be.
     * @param   H       The mapping matrix.
     * 
     * @warning         It is assumed that H is not null.
     */
    public abstract void getH(final Track2DPV track, Matrix H);
    
    /**
     * Returns the y vector which is the innovation (or residual) difference
     * between the observation and the mapped (to observation space) track state.
     * 
     * @param   track   The 2DPV track to indicate to the observation sub-class
     *                  how the innovation should be computed.
     * @param   H       The mapping matrix from the track space to the 
     *                  observation space.
     * @param   y       The innovation vector.
     * 
     * @warning         It is assumed that y is not null.
     */
    public abstract void getY(final Track2DPV track, final Matrix H, 
                              Matrix y);
    
    /**
     * Initializes a track with the observation data.
     * 
     * @param   track   The track to be updated with the observation data.
     * @param   x       The track state to be updated.
     * @param   P       The track covariance matrix to be updated.
     * 
     * @throws InvalidObsTrackPairException
     */
    public abstract void init(Track2DPV track, Matrix x, Matrix P) 
            throws InvalidObsTrackPairException;
    
    /**
     * Writes the observation to a data stream.
     * 
     * @param   stream   The stream to write the data to.
     * 
     * @throws IOException 
     */
    public void write(DataOutputStream stream) throws IOException
    {
        stream.writeInt(_id);
        stream.writeDouble(_initTimeSecs);
        stream.writeBoolean(_isTimeOnly);
        _z.write(stream);
        _R.write(stream);
    }
    
    /**
     * Reads the observation from a data stream.
     * 
     * @param   stream   The stream to read the data from.
     * 
     * @throws IOException 
     */
    public void read(DataInputStream stream) throws IOException
    {
        _id           = stream.readInt();
        _initTimeSecs = stream.readDouble();
        _isTimeOnly   = stream.readBoolean();
        _z.read(stream);
        _R.read(stream);
    }
    
    /**
     * Returns a refernce to the z observation vector which encapsulates all 
     * the data corresponding to the observed state.
     */
    public Matrix getZ()
    {
        return _z;
    }

    /**
     * Returns the R observation covariance matrix which corresponds to the z
     * observation vector.
     */
    public Matrix getR()
    {
        return _R;
    }
    
    /**
     * Returns the initialization time of the observation.
     */
    public double getInitTime()
    {
        return _initTimeSecs;
    }
    
    /**
     * Sets the initialization time of the observation.
     * 
     * @param   initTimeSecs   The observation initialization time (seconds).
     */
    public void setInitTime(final double initTimeSecs)
    {
        _initTimeSecs = initTimeSecs;
    }
    
    /**
     * Initializes the observation with the timestamp only, considering this
     * observation as time-only.
     * 
     * @param   initTimeSecs   The observation initialization time (seconds).
     */
    public void init(final double initTimeSecs)
    {
        _initTimeSecs = initTimeSecs;
        _isTimeOnly   = true;
    }
    
    /**
     * Sets the all data within the observation.
     * 
     * @param   z              The observation state vector.
     * @param   R              The observation uncertainty matrix.
     * @param   initTimeSecs   The initialization time (in seconds) of the observation.
     */
    public void init(final Matrix z, final Matrix R, final double initTimeSecs)
    {
        _initTimeSecs = initTimeSecs;
        _isTimeOnly   = true;
        
        try
        {
            _z.copy(z);
            _R.copy(R);
            _isTimeOnly = false;
        }
        catch(final Exception e)
        {
            System.out.println("Observation::set exception: " + e.toString());
        }
    }
    
    /**
     * Indicates whether the observation is time-only or not.
     * 
     * @param   isTimeOnly  Indicator for the time-only flag.
     */
    public void setIsTimeOnly(final boolean isTimeOnly)
    {
        _isTimeOnly = isTimeOnly;
    }
    
    /**
     * Returns whether the observation is time-only (true) or not (false) and so
     * has valid state and covariance data.
     */
    public boolean getIsTimeOnly()
    {
        return _isTimeOnly;
    }
    
    /**
     * Returns the next observation id.
     */
    protected int getNextObsId()
    {
        return NEXT_OBS_ID++;
    }
    
    //! The observed space state vector.
    protected Matrix _z;
    
    //! The observed space covariance or uncertainty matrix.
    protected Matrix _R;
    
    //! The time (in seconds) at which the observation was generated.
    protected double _initTimeSecs;
    
    //! A utility to write messages to a log.
    protected MessageLog _msgLog;
    
    //! Unique observation identifier.
    protected int _id;
    
    //! Indicates whether the observation is a time-only observation.
    /**
     * The observation is time-only if it only contains a valid timestamp and
     * and its state and covariance matrix are invalid.
     */
    protected boolean _isTimeOnly;
    
    //! Class observation identifier.
    private static int NEXT_OBS_ID = 1;
    
    //! Tag string identifying class, used for logging purposes.
    private static final String TAG = "Observation";
}
