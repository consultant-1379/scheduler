package com.distocraft.dc5000.etl.scheduler.trigger;

import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.distocraft.dc5000.etl.scheduler.TimeTrigger;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * @author savinen
 */
public class Interval extends TimeTrigger {

	 private Logger log = Logger.getLogger("scheduler.interval");

//	No need for constructor as compiler will generate this	
 /*public Interval() {
   super();
 }*/

  public boolean execute() throws SchedulerException {

    long time;
    update();
    
    // current time
    final Date curDate = new Date();

    final long curTime = curDate.getTime();
    
    //  interval Calculate milliseconds
    final long interval = (this.intervalHour * 60 * 60 * 1000) + (this.intervalMinute * 60 * 1000);
    
    // if last execution time is zero then this is the first execution
    if (this.lastExecutionTime != 0) {
      // if this is NOT the first execution so the time is retrieved from last
      // executon time

      time = this.lastExecutionTime;
    } else {
      // on first execution time is retrieved from execution time and interval
      // is subtracted form it so that the start time is executionTime (not
      // executionTime + interval)

      time = this.executionTime - interval;
    }

    log.finest("curTime value is - " + curTime + " and interval value is - " + interval + " and lastExecTime is - " + time);
    // Find the offset value  for LastExecutionTime  and  Current Time(server time) when DST start/End
    
    /* @autour : XSUTKUR TR: HP35271 
     * date : 08-02-2012(dd-mm-yyyy)
     * diffOffSet - Difference of Current time offset and Set Last execution offset
     * */
   
 //   time = this.lastExecutionTime;
         
    // if last execution time + interval is less or equal to current time. OR
    // if last execution time is in the future more than 1 hour, release the
    // trigger.


    if (curTime + (60 * 60 * 1000) < time) {
      this.lastExecutionTime = curTime;
      log.finest("New lastExecTime for condition - curTime + (60 * 60 * 1000) < time is " + this.lastExecutionTime);
      return true;
    } else if ((time + interval) <= curTime) {
      //    execution time ( the calculated, not the real) is stored to be put to
      // DB if execution is succesfull
      // this prevents the drift of the execution time */

      final long timeDifference = (curTime - time) % interval;
      this.lastExecutionTime = curTime - timeDifference;
      log.finest("New lastExecTime for condition - (time + interval) <= curTime is " + this.lastExecutionTime);
      return true;
    }

    return false;
    
  }

 }
