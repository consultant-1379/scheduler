/*
 * Created on 3.2.2005
 *
 */
package com.distocraft.dc5000.etl.scheduler.trigger;

import com.distocraft.dc5000.etl.scheduler.TimeTrigger;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * @author savinen
 * 
 * does nothing, just waits to be Triggered by direct commnad to the scheduler
 * 
 * 
 * 
 */
public class WaitToBeTriggered extends TimeTrigger {

  /**
   * constructor
   * 
   */

//	No need for constructor as compiler will generate this
  /*public WaitToBeTriggered() {
    super();
  }*/

  /**
   * 
   * 
   * 
   */
  public boolean execute() throws SchedulerException {

    return false;
  }

}
