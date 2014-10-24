/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage measures (i.e. distances, speeds, time).
 */
package com.madmath.measures;

import com.madmath.utilities.StringUtils;

/**
 * Validates the measure classes.
 */
public class MeasuresValidation 
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        Speed spd = new Speed(12345.678, Speed.SPEED_TYPE.METRES_PER_SEC);
        
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 5, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 2));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 1));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 6, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 7, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 8, 3));
        
        
        spd = new Speed(12345.6, Speed.SPEED_TYPE.METRES_PER_SEC);
        
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 5, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 2));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 1));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 6, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 7, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 8, 3));
        
        spd = new Speed(102, Speed.SPEED_TYPE.METRES_PER_SEC);
        
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 5, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 2));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 4, 1));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 6, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 7, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 8, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 2, 3));
        System.out.println(spd.toString(Speed.SPEED_TYPE.METRES_PER_SEC, 1, 3));
        
        
        Angle ang = new Angle(90.0, Angle.ANGLE_TYPE.DEGREES);
        System.out.println(ang.toString(Angle.ANGLE_TYPE.DEGREES_POS,3,0));
        
        ang = new Angle(45.0, Angle.ANGLE_TYPE.DEGREES);
        System.out.println(ang.toString(Angle.ANGLE_TYPE.DEGREES_POS,3,0));
        
        ang = new Angle(-90.0, Angle.ANGLE_TYPE.DEGREES);
        System.out.println(ang.toString(Angle.ANGLE_TYPE.DEGREES_POS,3,0));
        
        System.out.println(StringUtils.Pad(270, 3, 1));
        

    }

}
