package com.distocraft.dc5000.etl.scheduler.trigger;

import java.io.File;

import com.distocraft.dc5000.etl.scheduler.EventTrigger;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * @author savinen
 */
public class FileExists extends EventTrigger {

//	No need for constructor as compiler will generate this
  /*public FileExists() {
    super();
  }*/

  public boolean execute() throws SchedulerException {

    update();

    final File file = new File(this.trigger_command);

    if (file.exists()) {

      if (file.canWrite()) {

        if (!file.delete()) {
          throw new SchedulerException("File exists, but could not be writted/removed.");
        }

        return true;

      } else {
        throw new SchedulerException("File exists, but can not be writted/removed.");
      }

    }

    return false;
  }

}
