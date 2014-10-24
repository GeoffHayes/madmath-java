/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage measures (i.e. distances, speeds, time).
 */
package com.madmath.measures;

import com.madmath.utilities.StringUtils;

/**
 * Encapsulates a distance type of object that can be used to easily convert a
 * distance value from one unit type to another.
 */
public class Distance 
{
    //! Enumerated type to represent the different units for any given distance value.
    public enum DISTANCE_TYPE
    {
        KILOMETRES,    /**< Enumeration for kilometres. */
        METRES,        /**< Enumeration for metres. */
        MILES,         /**< Enumeration for miles. */
        YARDS,         /**< Enumeration for yards. */
        FEET,          /**< Enumeration for feet. */
        NAUTICAL_MILES /**< Enumeration for nautical miles. */
    };
    
    /**
     * Default class constructor.
     */
    public Distance()
    {
        this(0.0,DISTANCE_TYPE.METRES);
    }
    
    /**
     * Class copy constructor.
     * 
     * @param   copy   The data to copy.
     */
    public Distance(final Distance copy)
    {
        this(copy._distanceMetres,DISTANCE_TYPE.METRES);
    }
    
    /**
     * Class constructor to initialize the distance object data members.
     * 
     * @param   value  Distance value in units given by second parameter.
     * @param   units  Unit type for distance value.
     */
    public Distance(final double value, final DISTANCE_TYPE units)
    {
        _distanceMetres = convertToMetres(value, units);
    }
    
    /**
     * Sets the object with a new distance given the specified units.
     * 
     * @param   value  Distance value in units given by second parameter.
     * @param   units  Unit type for distance value.
     */
    public void set(final double value, final DISTANCE_TYPE units)
    {
        _distanceMetres = convertToMetres(value, units);
    }
    
    /**
     * Sets the object with a new distance given the default units of
     * metres.
     * 
     * @param   value  Distance value in units given by second parameter.
     */
    public void set(final double value)
    {
        _distanceMetres = convertToMetres(value, DISTANCE_TYPE.METRES);
    }
    
    /**
     * Copies the contents of one distance object to the other.
     * 
     * @param   copy   The data to copy.
     */
    public void copy(final Distance copy)
    {
        this._distanceMetres = copy._distanceMetres;
    }
    
    /**
     * Adds a distance to that stored within the object.
     * 
     * @param   value  Distance value in units given by second parameter.
     * @param   units  Unit type for distance value.
     */
    public void add(final double value, final DISTANCE_TYPE units)
    {
        final double distToAddMetres = convertToMetres(value,units);
        _distanceMetres += distToAddMetres;
    }
    
    /**
     * Gets the distance of the object in the default units of metres.
     * 
     * @retval  The distance of the object in metres.
     */
    public double get()
    {
        return _distanceMetres;
    }
    
    /**
     * Gets the distance of the object given the specified units.
     * 
     * @param   units   Unit type for distance value.
     * 
     * @retval  The distance of the object converted to the specified units.
     */
    public double get(final DISTANCE_TYPE units)
    {
        double convertedDistance = 0.0;
        
        switch (units)
        {
            case KILOMETRES:
            {
                convertedDistance = _distanceMetres/KM_TO_MT;
                break;
            }
            case METRES:
            {
                convertedDistance = _distanceMetres;
                break;
            }
            case MILES:
            {
                convertedDistance = _distanceMetres/MI_TO_MT;
                break;
            }
            case YARDS:
            {
                convertedDistance = _distanceMetres/YD_TO_MT;
                break;
            }
            case FEET:
            {
                convertedDistance = _distanceMetres/FT_TO_MT;
                break;
            }
            case NAUTICAL_MILES:
            {
                convertedDistance = _distanceMetres/NM_TO_MT;
            }
        }
        
        return convertedDistance;
    }
    
    @Override
    /**
     * Returns the textual string representation of the distance object.
     */
    public String toString()
    {
        return "Distance: " + this._distanceMetres + " metres";
    }
    
    /**
     * Writes the Distance type value to string given the unit type and format.
     * 
     * @param   units     Unit type for distance value.
     * @param   intSize   The number of integers in the integer portion, padded
     *                    to intSize.
     * @param   decSize   The number of integers in the decimal portion, padded
     *                    to decSize.
     */
    public String toString(final DISTANCE_TYPE units, 
                           final int intSize, 
                           final int decSize)
    {

        double distance = get(units);
        
        return StringUtils.Pad(distance, intSize, decSize);
    }
    
    /**
     * Function to convert the input distance in whatever units to metres.
     *
     * @param   value  Distance value in units given by second parameter.
     * @param   units  Unit type for distance value.
     * 
     * @retval  The input distance converted to metres.
     */
    private double convertToMetres(final double value, final DISTANCE_TYPE units)
    {
        double distanceInMetres = 0.0;
        
        switch (units)
        {
            case KILOMETRES:
            {
                distanceInMetres = value*KM_TO_MT;
                break;
            }
            case METRES:
            {
                distanceInMetres = value;
                break;
            }
            case MILES:
            {
                distanceInMetres = value*MI_TO_MT;
                break;
            }
            case YARDS:
            {
                distanceInMetres = value*YD_TO_MT;
                break;
            }
            case FEET:
            {
                distanceInMetres = value*FT_TO_MT;
                break;
            }
            case NAUTICAL_MILES:
            {
                distanceInMetres = value*NM_TO_MT;
            }
        }
        
        return distanceInMetres;
    }
    
    /**
     * Converts the distance units type to a string equivalent.
     * 
     * @param   units   The distance units type.
     */
    public static String ToString(final DISTANCE_TYPE units)
    {
        String unitsAsString = new String("");
        
        switch (units)
        {
            case KILOMETRES:
            {
                unitsAsString  = "kilometres";
                break;
            }
            case METRES:
            {
                unitsAsString  = "metres";
                break;
            }
            case MILES:
            {
                unitsAsString  = "miles";
                break;
            }
            case YARDS:
            {
                unitsAsString  = "yards";
                break;
            }
            case FEET:
            {
                unitsAsString  = "feet";
                break;
            }
            case NAUTICAL_MILES:
            {
                unitsAsString  = "nautical miles";
            }
        }
        
        return unitsAsString;
    }
    
    /**
     * Converts the distance units type to a string abreviation equivalent.
     * 
     * @param   units   The distance units type.
     */
    public static String ToStringAbbrev(final DISTANCE_TYPE units)
    {
        String unitsAsString = new String("");
        
        switch (units)
        {
            case KILOMETRES:
            {
                unitsAsString  = "km";
                break;
            }
            case METRES:
            {
                unitsAsString  = "m";
                break;
            }
            case MILES:
            {
                unitsAsString  = "mi";
                break;
            }
            case YARDS:
            {
                unitsAsString  = "yd";
                break;
            }
            case FEET:
            {
                unitsAsString  = "ft";
                break;
            }
            case NAUTICAL_MILES:
            {
                unitsAsString  = "nm";
            }
        }
        
        return unitsAsString;
    }
    
    //! The distance of the object in metres.
    double _distanceMetres;

    //! Conversion constant from kilometres to metres.
    private static final double KM_TO_MT = 1000.0;
    
    //! Conversion constant from miles to metres.
    private static final double MI_TO_MT = 1609.34;
    
    //! Conversion constant from yards to metres.
    private static final double YD_TO_MT = 0.9144;
    
    //! Conversion constant from feet to metres.
    private static final double FT_TO_MT = YD_TO_MT/3.0;
    
    //! Conversion constant from nautical miles to metres.
    private static final double NM_TO_MT = 1852.0;
    
}
