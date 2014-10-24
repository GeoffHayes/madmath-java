/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage measures (i.e. distances, speeds, time).
 */
package com.madmath.measures;

import com.madmath.utilities.StringUtils;

import java.lang.Math;

/**
 * Encapsulates an angle type of object that can be used to easily convert an
 * angle value from one unit type to another.
 */
public class Angle 
{
    //! Enumerated type to represent the different units for any given distance value.
    public enum ANGLE_TYPE
    {
        DEGREES,     /**< Enumeration for degrees. */
        DEGREES_POS, /**< Enumeration for positive degrees (0,360). */
        RADIANS      /**< Enumeration for radians. */
    };
    
    /**
     * Default class constructor.
     */
    public Angle()
    {
        this(0.0,ANGLE_TYPE.RADIANS);
    }
    
    /**
     * Class copy constructor.
     * 
     * @param   copy   The data to copy.
     */
    public Angle(final Angle copy)
    {
        this(copy._angleRads,ANGLE_TYPE.RADIANS);
    }
    
    /**
     * Class constructor to initialize the angle object data members.
     * 
     * @param   value  Angle value in units given by second parameter.
     * @param   units  Unit type for distance value.
     */
    public Angle(final double value, final ANGLE_TYPE units)
    {
        _angleRads = convertToRadians(value, units);
    }
    
    /**
     * Sets the object with a new angle given the specified units.
     * 
     * @param   value  Angle value in units given by second parameter.
     * @param   units  Unit type for angle value.
     */
    public void set(final double value, final ANGLE_TYPE units)
    {
        _angleRads = convertToRadians(value, units);
    }
    
    /**
     * Sets the object with a new angle given the default units of radians.
     * 
     * @param   value  Angle value in units given by second parameter.
     */
    public void set(final double value)
    {
        _angleRads = 
                convertToRadians(value, ANGLE_TYPE.RADIANS);
    }
    
    /**
     * Copies the contents of one angle object to the other.
     * 
     * @param   copy   The data to copy.
     */
    public void copy(final Angle copy)
    {
        this._angleRads = copy._angleRads;
    }
    
    /**
     * Gets the angle of the object in the default units of radians.
     * 
     * @retval  The angle of the object in radians.
     */
    public double get()
    {
        return _angleRads;
    }
    
    /**
     * Gets the angle of the object given the specified units.
     * 
     * @param   units   Unit type for angle value.
     * 
     * @retval  The angle of the object converted to the specified units.
     */
    public double get(final ANGLE_TYPE units)
    {
        double convertedAngle = 0.0;
        
        switch (units)
        {
            case DEGREES:
            {
                convertedAngle = _angleRads*RAD_TO_DEG;
                break;
            }
            case DEGREES_POS:
            {  
                convertedAngle = _angleRads;
                
                while (convertedAngle > 2.0*Math.PI)
                {
                    convertedAngle -= 2.0*Math.PI;
                }
                
                while(convertedAngle < 0.0)
                {
                    convertedAngle += 2.0*Math.PI;
                }
                
                convertedAngle *= RAD_TO_DEG;

                break;
            }
      
            case RADIANS:
            {
                convertedAngle = _angleRads;
                break;
            }
        }
        
        return convertedAngle;
    }
    
    @Override
    /**
     * Returns the textual string representation of the angle object.
     */
    public String toString()
    {
        return "Angle: " + this._angleRads + " metres";
    }
    
    /**
     * Writes the Angle type value to string given the unit type and format.
     * 
     * @param   units     Unit type for angle value.
     * @param   intSize   The number of integers in the integer portion, padded
     *                    to intSize.
     * @param   decSize   The number of integers in the decimal portion, padded
     *                    to decSize.
     */
    public String toString(final ANGLE_TYPE units, 
                           final int intSize, 
                           final int decSize)
    {
        final double angle = get(units);
        
        return StringUtils.Pad(angle, intSize, decSize);
    }
    
    /**
     * Function to convert the input angle in whatever units to radians.
     *
     * @param   value  Angle value in units given by second parameter.
     * @param   units  Unit type for angle value.
     * 
     * @retval  The input angle converted to radians.
     */
    private double convertToRadians(final double value, final ANGLE_TYPE units)
    {
        double angleInRads = 0.0;
        
        switch (units)
        {
            case DEGREES_POS:
            case DEGREES:
            {
                angleInRads = value*DEG_TO_RAD;
                break;
            }
            case RADIANS:
            {
                angleInRads = value;
                break;
            }
        }
        
        bind();
        
        return angleInRads;
    }
    
    /**
     * Adds an angle to that stored within the object.
     * 
     * @param   value  Angle value in units given by second parameter.
     * @param   units  Unit type for angle value.
     */
    public void add(final double value, final ANGLE_TYPE units)
    {
        final double angleToAddRads = convertToRadians(value,units);
        _angleRads += angleToAddRads;
        bind();

    }
    
    /**
     * Binds the angle in the interval [-PI,PI].
     */
    private void bind()
    {
        while(_angleRads < -Math.PI)
        {
            _angleRads += 2.0*Math.PI;
        }
        
        while(_angleRads > Math.PI)
        {
            _angleRads -= 2.0*Math.PI;
        } 
    }
    
    //! The angle of the object in radians.
    double _angleRads;

    //! Conversion constant from radians to degrees.
    private static final double RAD_TO_DEG = 180.0/Math.PI;
    
    //! Conversion constant from degrees to radians.
    private static final double DEG_TO_RAD = 1.0/RAD_TO_DEG;
}
