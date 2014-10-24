/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage geographic coordinate systems (classes, structures, algorithms).
 */
package com.madmath.geocoordinates;

/**
 * Validates the Position class.
 */
public class PositionValidation 
{

    public static void main(String[] args) 
    {
        Position pos = new Position(45, 45, Position.POS_VALUE_TYPE.DEGREES);
        String lat = pos.toStringLatitude();
        String lon = pos.toStringLongitude();

        System.out.println(lat + "  " + lon);
        
        pos = new Position(0, 0, Position.POS_VALUE_TYPE.DEGREES);
        lat = pos.toStringLatitude();
        lon = pos.toStringLongitude();

        System.out.println(lat + "  " + lon);
        
        pos = new Position(0, 179, Position.POS_VALUE_TYPE.DEGREES);
        lat = pos.toStringLatitude();
        lon = pos.toStringLongitude();

        System.out.println(lat + "  " + lon);
        
        pos = new Position(0, -179, Position.POS_VALUE_TYPE.DEGREES);
        lat = pos.toStringLatitude();
        lon = pos.toStringLongitude();
        
        System.out.println(lat + "  " + lon);
        
        pos = new Position(0, -179.92345, Position.POS_VALUE_TYPE.DEGREES);
        lat = pos.toStringLatitude();
        lon = pos.toStringLongitude();
        
        System.out.println(lat + "  " + lon);
        
        pos = new Position(-10, -180, Position.POS_VALUE_TYPE.DEGREES);
        lat = pos.toStringLatitude();
        lon = pos.toStringLongitude();

        System.out.println(lat + "  " + lon);


    }

}
