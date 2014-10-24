/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage target tracking structures (matrices) and algorithms.
 */
package com.madmath.tracking;

//!  Abstract class to describe prediction filters.
/**
 * A prediction filter is used to predict a track forward (or backward) in time
 * according to whatever algorithm the derived class has implemented.
 */
public abstract class PredictFilter 
{
    /**
     * Predicts the track state and covariance forward (or backward in time)
     * given the prediction time.
     * 
     * @param   track      The track whose state and covariance are to be predicted.
     * @param   timeSecs   The prediction time in seconds.
     * 
     * @retval  True if the prediction has been successful.
     * @retval  False if the prediction has been unsuccessful.
     */
    public abstract boolean predict(Track2DPV track, final double timeSecs);
}
