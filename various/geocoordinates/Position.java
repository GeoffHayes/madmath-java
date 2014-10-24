/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage geographic coordinate systems (classes, structures, algorithms).
 */
package com.madmath.geocoordinates;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.madmath.utilities.StringUtils;

//! Class to represent a geographic position given by a latitude and longitude.
public class Position 
{
    //! Enum to indicate the inputs or output units to an instance
    //! of this class.
    public enum POS_VALUE_TYPE
    {
        RADIANS, /**< enumeration for input/output as radians */
        DEGREES  /**< enumeration for input/output as degrees */
    };
    
    /**
     * Class constructor to initialize the latitude and longitude members.
     *
     * @param   lat   Position latitude in units matching input type.
     * @param   lon   Position longitude in units matching input type.
     * @param   units Units of input.
     */
    public Position(final double lat, final double lon, final POS_VALUE_TYPE units)
    {
        setLatitude(lat,units);
        setLongitude(lon,units);
    }
    
    /**
     * Default class constructor.
     */
    public Position()
    {
        this(0.0,0.0,POS_VALUE_TYPE.RADIANS);
    }
    
    /**
     * Class copy constructor.
     * 
     * @param   copy   The position to copy.
     */
    public Position(final Position copy)
    {
        _latitudeRads  = copy._latitudeRads;
        _longitudeRads = copy._longitudeRads;
    }

    /**
     * Returns the latitude in the requested units.
     *
     * @param   units  Units of requested latitude.
     *
     * @return  The latitude in the requested units.
     */
    public double getLatitude(final POS_VALUE_TYPE units)
    {
        double lat = _latitudeRads;

        switch (units)
        {
            case DEGREES:
            {
                lat = _latitudeRads*RAD_TO_DEG;
                break;
            }
            case RADIANS:
            {
                // nothing to do
                break;
            }
        }

        return lat;
    }

    /**
     * Returns the longitude in the requested units.
     *
     * @param   units  Units of requested longitude.
     *
     * @return  The longitude in the requested units.
     */
    public double getLongitude(final POS_VALUE_TYPE units)
    {
        double lon = _longitudeRads;

        switch (units)
        {
            case DEGREES:
            {
                lon = _longitudeRads*RAD_TO_DEG;
                break;
            }
            case RADIANS:
            {
                // nothing to do
                break;
            }
        }

        return lon;
    }

    /**
     * Sets the latitude in the requested units.
     *
     * @param   lat    Postion latitude to be set.
     * @param   units  Units of requested latitude.
     */
    public void setLatitude(final double lat, final POS_VALUE_TYPE units)
    {
        switch (units)
        {
            case DEGREES:
            {
                _latitudeRads = lat*DEG_TO_RAD;
                break;
            }
            case RADIANS:
            {
                _latitudeRads = lat;
                break;
            }
        }

        // ensure that the latitude is in the correct range
        if (_latitudeRads > M_PI_BY_2)
        {
            _latitudeRads = M_PI_BY_2;
        }
        else if (_latitudeRads < -M_PI_BY_2)
        {
            _latitudeRads = -M_PI_BY_2;
        }
    }
    
    @Override
    public String toString()
    {
        return new String(toStringLatitude() + " " + toStringLongitude());
    }
    
    /**
     * Returns the latitude in the following format:
     * 
     *  DD MM'SS.SS"N/S where DD is the degree, MM the minute;
     *  and SS the second (N(orth) or S(outh))
     */
    public String toStringLatitude()
    {
        final char degree = '\u00B0';
        
        double latDegs = _latitudeRads*RAD_TO_DEG;
        
        String latDir = new String("N");
        
        if(latDegs < 0.0)
        {
            latDir = "S";
            latDegs = Math.abs(latDegs);
        }

        // determine the latitude degrees, minutes, seconds
        double latTemp = latDegs - Math.floor(latDegs);
        double latMins = latTemp*((double)MAX_MINS);
        latTemp        = latMins - Math.floor(latMins);
        double latSecs = latTemp*((double)MAX_SECS);
        
        // convert to integers
        int iLatDegs = (int)Math.floor(latDegs);
        int iLatMins = (int)Math.floor(latMins);
        int iLatSecs = (int)Math.floor(latSecs);
        
        String strDecSecs = String.valueOf((latSecs-(double)iLatSecs)*100.0);
        strDecSecs = strDecSecs.substring(0, 2);
        
        if (iLatSecs==MAX_SECS)
        {
            iLatSecs = 0;
            iLatMins++;
        }
        
        if (iLatMins==MAX_MINS)
        {
            iLatMins = 0;
            iLatDegs++;
        }
        
        iLatDegs = Math.min(iLatDegs,MAX_DEGS_LAT);

        // update the strings
        return new String(StringUtils.Pad(iLatDegs, 2) + degree + StringUtils.Pad(iLatMins, 2) + "'" 
                + StringUtils.Pad(iLatSecs, 2) + "." + strDecSecs + "\"" + latDir);
    }

    /**
     * Returns the longitude in the following format:
     * 
     * DDD MM'SS.SS"E/W where DDD is the degree, MM the
     * minute, and SS the second (E(ast) or W(est))
     */
    public String toStringLongitude()
    {
        final char degree = '\u00B0';
        
        double lonDegs = _longitudeRads*RAD_TO_DEG;
       
        String lonDir = new String("E");

        if(lonDegs < 0.0)
        {
            lonDir = "W";
            lonDegs = Math.abs(lonDegs);
        }
        
        // determine the longitude degrees, minutes, seconds
        double lonTemp = lonDegs - Math.floor(lonDegs);
        double lonMins = lonTemp*((double)MAX_MINS);
        lonTemp        = lonMins - Math.floor(lonMins);
        double lonSecs = lonTemp*((double)MAX_SECS);
        
        // convert to integers
        int iLonDegs = (int)Math.floor(lonDegs);
        int iLonMins = (int)Math.floor(lonMins);
        int iLonSecs = (int)Math.floor(lonSecs);
        
        String strDecSecs = String.valueOf((lonSecs-(double)iLonSecs)*100.0);
        strDecSecs = strDecSecs.substring(0, 2);
        
        if (iLonSecs==MAX_SECS)
        {
            iLonSecs = 0;
            iLonMins++;
        }
        
        if (iLonMins==MAX_MINS)
        {
            iLonMins = 0;
            iLonDegs++;
        }
        
        iLonDegs = Math.min(iLonDegs,MAX_DEGS_LON);

        return new String(StringUtils.Pad(iLonDegs, 3) + degree + StringUtils.Pad(iLonMins, 2) + "'" 
                + StringUtils.Pad(iLonSecs, 2) + "." + strDecSecs + "\"" + lonDir);
    }
    
    /**
     * Writes the position to a data stream.
     * 
     * @param   stream   The stream to write the data to.
     * 
     * @throws IOException 
     */
    public void write(DataOutputStream stream) throws IOException
    {
        stream.writeDouble(_latitudeRads);
        stream.writeDouble(_longitudeRads);
    }
    
    /**
     * Reads the position from a data stream.
     * 
     * @param   stream   The stream to read the data from.
     * 
     * @throws IOException 
     */
    public void read(DataInputStream stream) throws IOException
    {
        _latitudeRads  = stream.readDouble();
        _longitudeRads = stream.readDouble();
    }
    
    /**
     * Returns the longitude in the requested units.
     *
     * @param   lon    Position longitude to be set.
     * @param   units  Units of requested longitude.
     *
     */
    public void setLongitude(final double lon, final POS_VALUE_TYPE units)
    {
        switch (units)
        {
            case DEGREES:
            {
                _longitudeRads = lon*DEG_TO_RAD;
                break;
            }
            case RADIANS:
            {
                _longitudeRads = lon;
                break;
            }
        }

        // ensure that the latitude is in the correct range
        if (_longitudeRads > M_PI)
        {
            _longitudeRads = M_PI;
        }
        else if (_longitudeRads < -M_PI)
        {
            _longitudeRads = -M_PI;
        }
    }
    
    /**
     * Copies the passed position object to self.
     * 
     * @param   copy   The position object to copy.
     */
    public void copy(final Position copy)
    {
        _latitudeRads  = copy._latitudeRads;
        _longitudeRads = copy._longitudeRads;
    }
    
    //! Position latitude in radians (native type) bounded by [-pi/2, pi/2]
    private double _latitudeRads;

    //! Position longitude in radians (native type) bounded by [-pi, pi]
    private double _longitudeRads;
    
    //! Alternative representation of PI.
    private final static double M_PI = java.lang.Math.PI;
    
    //! Alternative representation of PI/2.
    private final static double M_PI_BY_2 = M_PI/2.0;
    
    //! Conversion constant from radians to degrees.
    private final static double RAD_TO_DEG = 180.0/M_PI;
    
    //! Conversion constant from degrees to radians.
    private final static double DEG_TO_RAD = 1.0/RAD_TO_DEG;
    
    //! Constant for maximum number of seconds.
    private final static int MAX_SECS = 60;
    
    //! Constant for maximum number of minutes.
    private final static int MAX_MINS = 60;
    
    //! Constant for maximum number of degrees for a latitude.
    private final static int MAX_DEGS_LAT = 90;
    
    //! Constant for maximum number of degrees for a longitude.
    private final static int MAX_DEGS_LON = 180;

}
