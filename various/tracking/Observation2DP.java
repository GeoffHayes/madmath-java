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

import com.madmath.geocoordinates.Position;
import com.madmath.math.Matrix;
import com.madmath.utilities.MessageLog;

/**
 * The class encapsulates information concerning a two dimensional position
 * observation.
 */
public class Observation2DP extends Observation 
{
    /**
     * Default class constructor.
     */
    public Observation2DP()
    {
        super();
        
        _initTimeSecs =  0.0;
        _origin = new Position();
        _Hx     = new Matrix();

        try
        {
            _z.resize(DIMS);
            _R.resize(DIMS,DIMS);
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"Observation2DP exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    /**
     * Class copy constructor.
     * 
     * @param   copy   The observation to copy.
     */
    public Observation2DP(final Observation2DP copy)
    {
        super(copy);
        
        _initTimeSecs =  copy._initTimeSecs;
        _origin = new Position(copy._origin);
        _Hx     = new Matrix(); // not for copy

        try
        {
            _z.copy(copy._z);
            _R.copy(copy._R);
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"Observation2DP copy exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    /**
     * Copies the contents from one observation to the self observation.
     * 
     * @param   obsToCopy   The observation to copy.
     * @param   getNewId    Indicates whether a new id should be assigned to the
     *                      observation or not.
     */
    public void copy(final Observation2DP obsToCopy, final boolean getNewId)
    {
        _initTimeSecs =  obsToCopy._initTimeSecs;
        _isTimeOnly   = obsToCopy._isTimeOnly;
        _origin.copy(obsToCopy._origin);

        if(getNewId)
        {
            _id = getNextObsId();
        }
        
        try
        {
            _z.copy(obsToCopy._z);
            _R.copy(obsToCopy._R);
            _Hx.copy(obsToCopy._Hx);
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"Observation2DP::copy exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    /**
     * Sets the all data within the observation.
     * 
     * @param   z              The observation state vector.
     * @param   R              The observation uncertainty matrix.
     * @param   initTimeSecs   The initialization time (in seconds) of the observation.
     * @param   origin         The origin of the coordinate system that the state
     *                         vector z is relative to.
     */
    public void init(final Matrix z, final Matrix R, final double initTimeSecs, 
            final Position origin)
    {
        super.init(z, R, initTimeSecs);
        _origin.copy(origin);
    }
    
    @Override
    public void init(Track2DPV track, Matrix x, Matrix P) throws InvalidObsTrackPairException
    {
        try
        {
            x.setAt(XPOS,_z.at(XPOS));
            x.setAt(YPOS,_z.at(YPOS));
            
            P.setAt(XPOS,XPOS,_R.at(XPOS,XPOS));
            P.setAt(XPOS,YPOS,_R.at(XPOS,YPOS));
            P.setAt(YPOS,XPOS,_R.at(YPOS,XPOS));
            P.setAt(YPOS,YPOS,_R.at(YPOS,YPOS)); 
            
            track.setOrigin(_origin);
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
    public void getH(final Track2DPV track, Matrix H) 
    {
        // a track of the 2DPV space is being mapped to the 2D measurement space
        try
        {
            if(H.getRows() != Observation2DP.DIMS || H.getCols() != Track2DPV.DIMS)
            {
                H.resize(Observation2DP.DIMS,Track2DPV.DIMS);
            }

            H.setAt(XPOS,XPOS,1.0);
            H.setAt(YPOS,YPOS,1.0);
        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"getH exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    @Override
    public void getY(final Track2DPV track, final Matrix H, Matrix y)
    {
        try
        {
            _Hx.copy(H);
            final Matrix x = track.getX();
            _Hx.mult(x);

            y.copy(_z);
            y.sub(_Hx);

        }
        catch(final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"getY exception: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
    }
    
    @Override
    public void write(DataOutputStream stream) throws IOException
    {
        super.write(stream);
        
        // write the origin
        _origin.write(stream);
    }
    
    @Override
    public void read(DataInputStream stream) throws IOException
    {
        super.read(stream);
        
        // read the origin
        _origin.read(stream);
    }
    
    /**
     * Sets the origin.
     * 
     * @param   pos   The latitude and longitude origin of the coordinate system
     *                that the 2D position (z) is relative to.
     */
    public void set(final Position pos)
    {
        _origin.copy(pos);
    }
    
    /**
     * Returns the latitude and longitude origin position.
     */
    public Position getOrigin()
    {
        return _origin;
    }

    //! The latitude and longitude position that is the origin of which the 2DP state vector is relative to.
    private Position _origin;
    
    //! The vector mapped from the track space to the observation space, Hx.
    private Matrix _Hx;
    
    //! Indicates the number of dimensions in a 2DP state vector and uncertainty matrix.
    private static final int DIMS = 2;
    
    //! Indicates the position of the x-coordinate in the observed state and covariance "order".
    public static final int XPOS = 1;
    
    //! Indicates the position of the y-coordinate in the observed state and covariance "order".
    public static final int YPOS = 2;

    //! Tag string identifying class, used for logging purposes.
    private static final String TAG = "Observation2DP";
}
