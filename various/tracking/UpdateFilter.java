/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage target tracking structures (matrices) and algorithms.
 */
package com.madmath.tracking;

//!  Abstract class to describe update filters.
/**
 * An update filter is used to update a track with an observation
 * according to whatever algorithm the derived class has implemented.
 */
public abstract class UpdateFilter 
{
    /**
     * Updates the track state and covariance given the observation.
     * 
     * @param   track   The track whose state and covariance are to be updated.
     * @param   obs     The observation used to update the track.
     * 
     * @retval  True if the update has been successful.
     * @retval  False if the update has been unsuccessful.
     */
    public abstract boolean update(Track2DPV track, final Observation obs);
}
