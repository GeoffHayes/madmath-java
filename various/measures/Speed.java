/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage math-centric structures (matrices) and algorithms.
 */
package com.madmath.measures;

import com.madmath.utilities.StringUtils;

/**
 * Encapsulates a speed type of object that can be used to easily convert a
 * speed value from one unit type to another.
 */
public class Speed 
{
    //! Enumerated type to represent the different units for any given speed value.
    public enum SPEED_TYPE
    {
        KILOMETRES_PER_HOUR,  /**< Enumeration for kilometres per hour. */
        METRES_PER_SEC,       /**< Enumeration for metres per second. */
        MILES_PER_HOUR,       /**< Enumeration for miles per hour. */
        YARDS_PER_SEC,        /**< Enumeration for yards per second. */
        FEET_PER_SEC,         /**< Enumeration for feet per second. */
        KNOTS                 /**< Enumeration for knots (nautical miles per hour). */
    };
    
    /**
     * Default class constructor.
     */
    public Speed()
    {
        this(0.0,SPEED_TYPE.METRES_PER_SEC);
    }
    
    /**
     * Class copy constructor.
     * 
     * @param   copy   The data to copy.
     */
    public Speed(final Speed copy)
    {
        this(copy._speedMetresPerSec,SPEED_TYPE.METRES_PER_SEC);
    }
    
    /**
     * Class constructor to initialize the speed object data members.
     * 
     * @param   value  Speed value in units given by second parameter.
     * @param   units  Unit type for speed value.
     */
    public Speed(final double value, final SPEED_TYPE units)
    {
        _speedMetresPerSec = convertToMetresPerSecond(value, units);

    }
    
    /**
     * Sets the object with a new speed given the specified units.
     * 
     * @param   value  Speed value in units given by second parameter.
     * @param   units  Unit type for speed value.
     */
    public void set(final double value, final SPEED_TYPE units)
    {
        _speedMetresPerSec = convertToMetresPerSecond(value, units);
    }
    
    /**
     * Sets the object with a new speed given the default units of
     * metres per second.
     * 
     * @param   value  Speed value in units given by second parameter.
     */
    public void set(final double value)
    {
        _speedMetresPerSec = 
                convertToMetresPerSecond(value, SPEED_TYPE.METRES_PER_SEC);
    }
    
    /**
     * Gets the speed of the object in the default units of metres per second.
     * 
     * @retval  The speed of the object in metres per second.
     */
    public double get()
    {
        return _speedMetresPerSec;
    }
    
    /**
     * Copies the contents of one speed object to the other.
     * 
     * @param   copy   The data to copy.
     */
    public void copy(final Speed copy)
    {
        this._speedMetresPerSec = copy._speedMetresPerSec;
    }
    
    /**
     * Gets the speed of the object given the specified units.
     * 
     * @param   units   Unit type for speed value.
     * 
     * @retval  The speed of the object converted to the specified units.
     */
    public double get(final SPEED_TYPE units)
    {
        double convertedSpeed = 0.0;
        
        switch (units)
        {
            case KILOMETRES_PER_HOUR:
            {
                convertedSpeed = _speedMetresPerSec/KMPHR_TO_MTPSEC;
                break;
            }
            case METRES_PER_SEC:
            {
                convertedSpeed = _speedMetresPerSec;
                break;
            }
            case MILES_PER_HOUR:
            {
                convertedSpeed = _speedMetresPerSec/MIPHR_TO_MTPSEC;
                break;
            }
            case YARDS_PER_SEC:
            {
                convertedSpeed = _speedMetresPerSec/YDPSEC_TO_MTPSEC;
                break;
            }
            case FEET_PER_SEC:
            {
                convertedSpeed = _speedMetresPerSec/FTPSEC_TO_MTPSEC;
                break;
            }
            case KNOTS:
            {
                convertedSpeed = _speedMetresPerSec/KNOTS_TO_MTPSEC;
            }
        }
        
        return convertedSpeed;
    }
    
    @Override
    /**
     * Returns the textual string representation of the speed object.
     */
    public String toString()
    {
        return "Speed: " + this._speedMetresPerSec + " metres/second";
    }
    
    /**
     * Function to convert the input speed in whatever units to metres per second.
     *
     * @param   value  Speed value in units given by second parameter.
     * @param   units  Unit type for speed value.
     * 
     * @retval  The input speed converted to metres per second.
     */
    private double convertToMetresPerSecond(final double value, final SPEED_TYPE units)
    {
        double speedInMtrsPerSec = 0.0;
        
        switch (units)
        {
            case KILOMETRES_PER_HOUR:
            {
                speedInMtrsPerSec = value*KMPHR_TO_MTPSEC;
                break;
            }
            case METRES_PER_SEC:
            {
                speedInMtrsPerSec = value;
                break;
            }
            case MILES_PER_HOUR:
            {
                speedInMtrsPerSec = value*MIPHR_TO_MTPSEC;
                break;
            }
            case YARDS_PER_SEC:
            {
                speedInMtrsPerSec = value*YDPSEC_TO_MTPSEC;
                break;
            }
            case FEET_PER_SEC:
            {
                speedInMtrsPerSec = value*FTPSEC_TO_MTPSEC;
                break;
            }
            case KNOTS:
            {
                speedInMtrsPerSec = value*KNOTS_TO_MTPSEC;
            }
        }
        
        return speedInMtrsPerSec;
    }
    
    /**
     * Writes the Speed type value to string given the unit type and format.
     * 
     * @param   units     Unit type for speed value.
     * @param   intSize   The number of integers in the integer portion, padded
     *                    to intSize.
     * @param   decSize   The number of integers in the decimal portion, padded
     *                    to decSize.
     */
    public String toString(final SPEED_TYPE units, 
                           final int intSize, 
                           final int decSize)
    {

        double speed = get(units);
        
        return StringUtils.Pad(speed, intSize, decSize);
    }
    
    /**
     * Converts the speed units type to a string equivalent.
     * 
     * @param   units   The speed units type.
     */
    public static String ToString(final SPEED_TYPE units)
    {
        String unitsAsString = new String("");
        
        switch (units)
        {
            case KILOMETRES_PER_HOUR:
            {
                unitsAsString  = "kilometres per hour";
                break;
            }
            case METRES_PER_SEC:
            {
                unitsAsString  = "metres per second";
                break;
            }
            case MILES_PER_HOUR:
            {
                unitsAsString  = "miles per hour";
                break;
            }
            case YARDS_PER_SEC:
            {
                unitsAsString  = "yards per second";
                break;
            }
            case FEET_PER_SEC:
            {
                unitsAsString  = "feet per second";
                break;
            }
            case KNOTS:
            {
                unitsAsString  = "knots";
            }
        }
        
        return unitsAsString;
    }
    
    /**
     * Converts the speed units type to a string abreviation equivalent.
     * 
     * @param   units   The speed units type.
     */
    public static String ToStringAbbrev(final SPEED_TYPE units)
    {
        String unitsAsString = new String("");
        
        switch (units)
        {
        case KILOMETRES_PER_HOUR:
        {
            unitsAsString  = "kmph";
            break;
        }
        case METRES_PER_SEC:
        {
            unitsAsString  = "mps";
            break;
        }
        case MILES_PER_HOUR:
        {
            unitsAsString  = "miph";
            break;
        }
        case YARDS_PER_SEC:
        {
            unitsAsString  = "ydps";
            break;
        }
        case FEET_PER_SEC:
        {
            unitsAsString  = "ftps";
            break;
        }
        case KNOTS:
        {
            unitsAsString  = "kts";
        }
        }
        
        return unitsAsString;
    }
    
    //! The speed of the object in metres per second.
    private double _speedMetresPerSec;

    //! Conversion constant from kilometres per hour to metres per second.
    private static final double KMPHR_TO_MTPSEC = 1000.0/3600.0;
    
    //! Conversion constant from miles per hour to metres per second.
    private static final double MIPHR_TO_MTPSEC = 1609.34/3600.0;
    
    //! Conversion constant from yards per second to metres per second.
    private static final double YDPSEC_TO_MTPSEC = 0.9144;
    
    //! Conversion constant from feet per second to metres per second.
    private static final double FTPSEC_TO_MTPSEC = YDPSEC_TO_MTPSEC/3.0;
    
    //! Conversion constant from knots to metres per second.
    private static final double KNOTS_TO_MTPSEC = 1852.0/3600.0;
    
}
