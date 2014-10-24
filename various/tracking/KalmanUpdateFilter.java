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
public class KalmanUpdateFilter extends UpdateFilter
{
    /**
     * Default class constructor.
     */
    public KalmanUpdateFilter()
    {
        _H  = new Matrix();
        _Ht = new Matrix();
        _y  = new Matrix();
        _S  = new Matrix();
        _Si = new Matrix();
        _K  = new Matrix();

        MessageLog.GetInstance(_msgLog);
    }
    
    /**
     * Returns the mapping matrix H.
     */
    public Matrix getH()
    {
        return _H;
    }
    
    /**
     * Returns the innovation vector y.
     */
    public Matrix getY()
    {
        return _y;
    }
    
    /**
     * Returns the innovation covariance matrix S.
     */
    public Matrix getS()
    {
        return _S;
    }
    
    /**
     * Returns the Kalman gain matrix K.
     */
    public Matrix getK()
    {
        return _K;
    }
    
    @Override
    public boolean update(Track2DPV track, final Observation obs)
    {
        boolean success = false;
        
        try
        {
            Matrix x = track.getX();
            Matrix P = track.getP();

            // get the H matrix
            obs.getH(track, _H);
            
            // get the innovation y vector
            obs.getY(track,  _H,  _y);

            // compute the innovation covariance matrix S=HPHt + R
            _Ht.copy(_H);
            _Ht.t();
            
            final Matrix R = obs.getR();
            
            _S.copy(_H);
            _S.mult(P);
            _S.mult(_Ht);
            _S.add(R);
            
            // compute the Kalman gain K=PHtSi
            _Si.copy(_S);
            _Si.i();

            _K .copy(P);
            _K.mult(_Ht);  
            _K.mult(_Si);
            
            // update the state vector x=x+Ky
            Matrix Ky = new Matrix(_K);
            Ky.mult(_y);
            x.add(Ky);
            
            // update the covariance matrix P=(I-KH)P(I-KH)t + KRKt
            Matrix KH = new Matrix(_K);
            KH.mult(_H);
            
            Matrix I = new Matrix(P.getRows(),P.getCols());
            I.setAsIdentity();
            
            Matrix IKH = new Matrix(I);
            IKH.sub(KH);
            
            Matrix IKHt = new Matrix(IKH);
            IKHt.t();
            
            Matrix Kt = new Matrix(_K);
            Kt.t();
            
            Matrix KRKt = new Matrix(_K);
            KRKt.mult(R);
            KRKt.mult(Kt);
            
            Matrix Pp = new Matrix(IKH);
            Pp.mult(P);
            Pp.mult(IKHt);
            Pp.add(KRKt);
            P.copy(Pp);
            
            success = true;
        }
        catch (final Exception e)
        {
            if(_msgLog != null)
            {
                _msgLog.writeMessage(TAG,"update: " + e.toString(),
                                     MessageLog.MESSAGE_SEVERITY_LEVEL.ERROR);
            }
        }
        
        return success;
    }

    //! The mapping matrix from the true track space to the observation space.
    private Matrix _H;
    
    //! The transpose of the mapping matrix, H.
    private Matrix _Ht;
    
    //! The innovation or residual vector of the observed state and the mapped track state.
    private Matrix _y;
    
    //! The innovation covariance matrix.
    private Matrix _S;
    
    //! The inverse of the innovation covariance matrix, S.
    private Matrix _Si;
    
    //! The Kalman gain matrix.
    private Matrix _K;
    
    //! A utility to write messages to a log.
    private MessageLog _msgLog;
    
    //! Tag string identifying class, used for logging purposes.
    private static final String TAG = "KalmanUpdateFilter";
}
