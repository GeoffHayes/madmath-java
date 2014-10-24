/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage the SelfTrackerApp activity and supporting classes.
 */
package com.madmath.selftrackerapp;

import com.madmath.tracking.Observation2DP;

/**
 * Class that encapsulates data that is sent from the runnable "task" to the
 * parent activity.  This data includes the latest position of the user with
 * respect to some origin (enclosed within an observation object) and the time
 * at which the data package was generated.
 *
 * @note   There need not be an observation contained within the data packet
 *         in the event that a new position cannot be obtained.  However a
 *         timestamp is required so that the parent activity still receives a
 *         packet.
 */
public class ObservationPacket
{
    /**
     * Default class constructor.
     */
    public ObservationPacket()
    {
        this.init(0.0);
    }
    
    /**
     * Initializes the packet.
     * 
     * @param   timestampSecs  The timestamp (seconds) at which the packet was
     *                         initialized.
     *                         
     * @note    The observation data is set to null.
     */
    public void init(final double timestampSecs)
    {
        _timestampSecs = timestampSecs;
        _obs2DP        = null;
    }
    
    /**
     * Initializes the packet.
     * 
     * @param   timestampSecs  The timestamp (seconds) at which the packet was
     *                         initialized.
     * @param   obs2DP         The observation to update the packet with.
     */
    public void init(final double timestampSecs, final Observation2DP obs2DP)
    {
        _timestampSecs = timestampSecs;
        _obs2DP        = new Observation2DP(obs2DP);
    }
    
    /**
     * Returns the packet initialization timestamp in seconds.
     */
    public double getTimestamp()
    {
        return _timestampSecs;
    }
    
    /**
     * Returns the packet observation data.
     */
    public Observation2DP getObservation()
    {
        return _obs2DP;
    }
    
    //! Timestamp in seconds that the packet was generated at.
    private double _timestampSecs;
    
    //! The observation position data (relative to an origin).
    private Observation2DP _obs2DP;
}
