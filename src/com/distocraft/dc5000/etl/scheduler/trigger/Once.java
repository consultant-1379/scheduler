package com.distocraft.dc5000.etl.scheduler.trigger;

import java.util.Date;

import com.distocraft.dc5000.etl.scheduler.Scheduler;
import com.distocraft.dc5000.etl.scheduler.TimeTrigger;

import com.ericsson.eniq.scheduler.exception.SchedulerException;
/**
 * @author savinen
 * 
 */
public class Once extends TimeTrigger {

//	No need for constructor as compiler will generate this	
  /*public Once() {
    super();
  }*/

  public boolean execute() throws SchedulerException {

    update();

    // if last execution time is less or equal to current time

    if (((this.executionTime) <= (new Date()).getTime())
        && (this.status == null || (!this.status.equalsIgnoreCase(Scheduler.STATUS_EXECUTED)))) {
      return true;
    }

    return false;
  }

}
