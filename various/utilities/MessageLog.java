/*
 * 2013-09-26   Geoff Hayes     Initial Release.
 */

/**
 * Package to manage miscellaneous utilities.
 */
package com.madmath.utilities;

//! Singleton class used for messaging.
/**
 * Assumed instantiated in the main program before all other objects that may
 * may use of logging.  As such, the main application will define the logging
 * method (to System.out, Android, etc.) and the message level.
 */
public class MessageLog 
{
    //! Enum to indicate the message output level, ordered from least to greatest severity.
    public enum MESSAGE_SEVERITY_LEVEL
    {
        VERBOSE,  /**< Enumeration for verbose message */
        WARNING,  /**< Enumeration for warning message */
        ERROR     /**< Enumeration for error message   */
    };
    
    //! Enum to indicate the output log type.
    public enum LOG_TYPE
    {
        SYSTEM, /**< Messages written to System.out.println.     */
        ANDROID /**< Messages written to android.util.Log.* log. */
    }
    
    /**
     * Returns the message logging instance.
     * 
     * @param   log   Message log to be set with the singleton.
     * 
     * @retval  true  Indicates if the caller is the creator of the instance
     */
    public static void GetInstance(MessageLog log)
    {
        if (_messageLog == null)
        {
            _messageLog = new MessageLog();
        }
        
        log = _messageLog;
    }
    
    /**
     * Sets the message output level.
     * 
     * @param   msgLevel   The message output level.
     */
    public void setMsgLevel(final MESSAGE_SEVERITY_LEVEL msgLevel)
    {
        _msgLevel = msgLevel;
    }
    
    /**
     * Sets the message output log type.
     * 
     * @param   logType   The message output log type.
     */
    public void setLogType(final LOG_TYPE logType)
    {
        _logType = logType;
    }
    
    /**
     * Writes the message to the log given the message (severity) level.
     * 
     * @param   tag       A tag corresponding to the message (typically class 
     *                    name).
     * @param   msg       The message to write to the log.
     * @param   msgLevel  The message level.
     */
    public void writeMessage(final String tag, final String msg, 
                             final MESSAGE_SEVERITY_LEVEL msgLevel)
    {
        if (msgLevel.compareTo(_msgLevel) >= 0)
        {
            switch(_logType)
            {
                case SYSTEM:
                {
                    System.out.println(tag + ":" + msg);
                    break;
                }
                case ANDROID:
                {
//                    switch (msgLevel)
//                    {
//                        case VERBOSE:
//                        {
//                            android.util.Log.v(tag, msg);
//                            break;
//                        }
//                        case WARNING:
//                        {
//                            android.util.Log.w(tag, msg);
//                            break;
//                        }
//                        case ERROR:
//                        {
//                            android.util.Log.e(tag, msg);
//                            break;
//                        }
//                    }
                    break;
                }
            }
        }
    }

    /**
     * Protected class constructor.
     */
    public MessageLog()
    {
        _msgLevel = MESSAGE_SEVERITY_LEVEL.ERROR;
        _logType  = LOG_TYPE.SYSTEM;
    }
    
    //! The singleton logging instance to be shared by all callers within the app.
    private static MessageLog _messageLog = null;
    
    //! The message output level.
    /**
     * Any message whose output level is greater than or equal to the set level, is
     * written to the log.
     */
    MESSAGE_SEVERITY_LEVEL _msgLevel;
    
    //! The message output log type.
    LOG_TYPE _logType;
};
