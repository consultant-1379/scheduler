/*
 * Created on 20.4.2006
 *
 */
package com.distocraft.dc5000.etl.scheduler.trigger;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import com.distocraft.dc5000.etl.scheduler.TimeTrigger;

import com.ericsson.eniq.scheduler.exception.SchedulerException;
/**
 * This class checks for weekly intervalled schedules and if needed executes
 * them.
 * 
 * @author berggren
 */
public class WeeklyInterval extends TimeTrigger {

  protected Integer intervalStartHour;

  protected Integer intervalStartMinute;

  protected Integer intervalEndHour;

  protected Integer intervalEndMinute;

  /**
   * constructor
   * 
   */
//	No need for constructor as compiler will generate this
  /*public WeeklyInterval() {
    super();
  }*/

  /**
   * Checks if the schedule should be executed. If a set execution is needed,
   * this method returns true. If set execution is not needed, this method
   * returns false.
   * 
   * @return Returns true if execution is needed, otherwise returns false.
   * 
   */
  public boolean execute() throws SchedulerException {

    long time;
    update();

    // current time
    final Date curDate = new Date();
    final long curTime = curDate.getTime();

    /* interval Calculate milliseconds */
    final long interval = (this.intervalHour * 60 * 60 * 1000) + (this.intervalMinute * 60 * 1000);

    // Get/parse the values from database to these variables from
    // this.OSCommand.
    final String serializedIntervalString = this.OSCommand;

    final Properties intervalProps = new Properties();

    if (serializedIntervalString != null && serializedIntervalString.length() > 0) {

      try {
        final ByteArrayInputStream bais = new ByteArrayInputStream(serializedIntervalString.getBytes());
        intervalProps.load(bais);
        bais.close();
      } catch (Exception e) {
      }
    }

    if (intervalProps.getProperty("intervalStartHour") == null
        || intervalProps.getProperty("intervalStartHour").equals("")) {
      intervalProps.setProperty("intervalStartHour", "0");
    }

    if (intervalProps.getProperty("intervalStartMinute") == null
        || intervalProps.getProperty("intervalStartMinute").equals("")) {
      intervalProps.setProperty("intervalStartMinute", "0");
    }

    if (intervalProps.getProperty("intervalEndHour") == null || intervalProps.getProperty("intervalEndHour").equals("")) {
      intervalProps.setProperty("intervalEndHour", "0");
    }

    if (intervalProps.getProperty("intervalEndMinute") == null
        || intervalProps.getProperty("intervalEndMinute").equals("")) {
      intervalProps.setProperty("intervalEndMinute", "0");
    }

    intervalStartHour = new Integer(intervalProps.getProperty("intervalStartHour"));
    intervalStartMinute = new Integer(intervalProps.getProperty("intervalStartMinute"));
    intervalEndHour = new Integer(intervalProps.getProperty("intervalEndHour"));
    intervalEndMinute = new Integer(intervalProps.getProperty("intervalEndMinute"));

    if (this.lastExecutionTime != 0) {

      // if this is NOT the first execution so the time is retrieved from last
      // executon time
      time = this.lastExecutionTime;

    } else {

      // on first execution time is retrieved from execution time and interval
      // is subtracted from it so that the start time is executionTime (not
      // executionTime + interval)
      time = this.executionTime - interval;

    }

    final GregorianCalendar exCal = new GregorianCalendar();
    exCal.setTimeInMillis(time);

    /* current date */
    final GregorianCalendar curCal = new GregorianCalendar();
    curCal.setTimeInMillis(curTime);

    // if last execution time is in the future more than 1 hour, release the
    // trigger.
    if ((curTime + (60 * 60 * 1000) < time)) {

      this.lastExecutionTime = curTime;
      return true;

    }

    // check for correct day
    boolean day = false;
    if (curCal.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.MONDAY
        && schedule.getMon_flag().equalsIgnoreCase("y")){
      day = true;
    }
    if (curCal.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.TUESDAY
        && schedule.getTue_flag().equalsIgnoreCase("y")){
      day = true;
    }
    if (curCal.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.WEDNESDAY
        && schedule.getWed_flag().equalsIgnoreCase("y")){
      day = true;
    }
    if (curCal.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.THURSDAY
        && schedule.getThu_flag().equalsIgnoreCase("y")){
      day = true;
    }
    if (curCal.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.FRIDAY
        && schedule.getFri_flag().equalsIgnoreCase("y")){
      day = true;
    }
    if (curCal.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SATURDAY
        && schedule.getSat_flag().equalsIgnoreCase("y")){
      day = true;
    }
    if (curCal.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SUNDAY
        && schedule.getSun_flag().equalsIgnoreCase("y")){
      day = true;
    }

    // Check if the day is correct
    if (day) {
      // The time is within intervaltime
      if (curCal.get(GregorianCalendar.HOUR_OF_DAY) >= intervalStartHour.intValue()
          && curCal.get(GregorianCalendar.HOUR_OF_DAY) <= intervalEndHour.intValue()) {

        if (curCal.get(GregorianCalendar.HOUR_OF_DAY) == intervalStartHour.intValue()
            && curCal.get(GregorianCalendar.MINUTE) <= intervalStartMinute.intValue()) {
          // Do nothing... The interval starting minutes are not yet full.
          return false;
        }

        else if (curCal.get(GregorianCalendar.HOUR_OF_DAY) == intervalEndHour.intValue()
            && curCal.get(GregorianCalendar.MINUTE) >= intervalEndMinute.intValue()) {
          // Do nothing... The interval ending minute limit is exceeded.
          return false;
        } else {
          // if last execution time + interval is less or equal to current time
          if ((time + interval) <= curTime) {
            // execution time ( the calculated, not the real) is stored to be
            // put to DB if execution is succesfull

            // this prevents the drift of the execution time

            final long timeDifference = (curTime - time) % interval;
            this.lastExecutionTime = curTime - timeDifference;

            return true;
          }
        }
      }

    }
    return false;
  }

}
