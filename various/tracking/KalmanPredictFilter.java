/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage target tracking structures (matrices) and algorithms.
 */
package com.madmath.tracking;

import com.madmath.math.Matrix;
import com.madmath.utilities.MessageLog;

//! Track prediction algorithms that are specific to the Kalman filter.
/**
 * An instance of this class is used to predict tracks forward or backward in
 * time according to the type of the track as per the Kalman (linear or extended)
 * filtering algorithm definitions.
 */
public class KalmanPredictFilter extends PredictFilter
{
    /**
     * Default class constructor.
     */
    public KalmanPredictFilter()
    {
       this(0.0);
    }
    
    /**
     * Class constructor.
     * 
     * @param   q   Process noise (unitless).
     */
    public KalmanPredictFilter(final double q)
    {
        _F  = new Matrix();
        _temp = new Matrix();
        _Q  = new Matrix();
        _xp = new Matrix();
        _Pp = new Matrix();
        _q  = q;
        
        MessageLog.GetInstance(_msgLog);
    }
    
    /**
     * Returns the transition matrix F.
     */
    public Matrix getF()
    {
        return _F;
    }
    
    /**
     * Returns the process noise matrix Q.
     */
    public Matrix getQ()
    {
        return _Q;
    }
    
    /**
     * Returns the plant noise scalar.
     */
    public double getQNoise()
    {
        return _q;
    }
    
    /**
     * Returns the predicted track state vector.
     */
    public Matrix getX()
    {
        return _xp;
    }
    
    /**
     * Returns the predicted track covariance matrix.
     */
    public Matrix getP()
    {
        return _Pp;
    }
    
    @Override
    public boolean predict(Track2DPV track, final double timeSecs) 
    {
        boolean success = false;
        
        try
        {
            Matrix x = track.getX();
            Matrix P = track.getP();

            // construct the transition matrix for the 2DPV track
            _F.resize(P.getRows(), P.getCols());
            _F.setAsIdentity();
            _F.setAt(Track2DPV.XPOS,Track2DPV.VX, timeSecs);
            _F.setAt(Track2DPV.YPOS,Track2DPV.VY, timeSecs);

            // construct the process noise matrix for the 2DPV track
            _Q.resize(P.getRows(), P.getCols());
            _Q.assign(0.0);
            
            final double timeSecsSqrd = timeSecs*timeSecs;
            final double timeSecsCbd  = timeSecsSqrd*timeSecs;

            _Q.setAt(Track2DPV.XPOS, Track2DPV.XPOS, timeSecsCbd/3.0);
            _Q.setAt(Track2DPV.YPOS, Track2DPV.YPOS, timeSecsCbd/3.0);
            _Q.setAt(Track2DPV.XPOS, Track2DPV.VX,   timeSecsSqrd/2.0);
            _Q.setAt(Track2DPV.VX,   Track2DPV.XPOS, timeSecsSqrd/2.0);
            _Q.setAt(Track2DPV.YPOS, Track2DPV.VY,   timeSecsSqrd/2.0);
            _Q.setAt(Track2DPV.VY,   Track2DPV.YPOS, timeSecsSqrd/2.0);
            _Q.setAt(Track2DPV.VX,   Track2DPV.VX,   timeSecs);
            _Q.setAt(Track2DPV.VY,   Track2DPV.VY,   timeSecs);
            _Q.mult(_q);
            
            // construct the predicted state vector and covariance matrix
            _xp.resize(x.getRows(), x.getCols());
            _xp.assign(0.0);
            
            _Pp.resize(P.getRows(), P.getCols());

            // predict the covariance matrix according to P'=FPFt+Q
            _temp.copy(_F);
            _temp.t();
            
            _Pp.copy(_F);
            _Pp.mult(P);
            _Pp.mult(_temp);
            _Pp.add(_Q);
            
            // predict the state vector according to x'=Fx
            _temp.copy(_F);
            _temp.mult(x);
            _xp.copy(_temp);
            
            // copy the predicted state and covariance to the track state and covariance
            x.copy(_xp);
            P.copy(_Pp);  
            
            success = true;
        }
        catch (final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"predict: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
        
        return success;
    }

    //! Transition matrix used to predict the track state and covariance matrix in time.
    private Matrix _F;
    
    //! A temporary matrix for various operations..
    private Matrix _temp;
    
    //! Process or plant noise matrix added to the track covariance matrix to prevent premature convergence.
    private Matrix _Q;
    
    //! Predicted state vector (x' = Fx).
    private Matrix _xp;
    
    //! Predicted covariance matrix (P' = FPFt + qQ).
    private Matrix _Pp;
    
    //! Process or plant noise scalar.
    private double _q;
    
    //! A utility to write messages to a log.
    private MessageLog _msgLog;
    
    //! Tag string identifying class, used for logging purposes.
    private static final String TAG = "KalmanPredictFilter";
}
