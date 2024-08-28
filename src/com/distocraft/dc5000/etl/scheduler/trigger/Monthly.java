package com.distocraft.dc5000.etl.scheduler.trigger;

import java.util.Date;
import java.util.GregorianCalendar;

import com.distocraft.dc5000.etl.scheduler.TimeTrigger;
import com.ericsson.eniq.scheduler.exception.SchedulerException;
/**
 * Checks for day of the month and execution time.
 * 
 * @author savinen
 */
public class Monthly extends TimeTrigger {

//	No need for constructor as compiler will generate this
	/*public Monthly() {
    super();
  }*/

  public boolean execute() throws SchedulerException {
    boolean first = false;
    long time;
    update();
    

    if (this.lastExecutionTime != 0) {
      // last executed time
      time = this.lastExecutionTime;

    } else {

      // last executed time
      time = this.executionTime;
      first = true;
    }

    final GregorianCalendar exCal = new GregorianCalendar();
    exCal.setTimeInMillis(time);

    // current date
    final GregorianCalendar curCal = new GregorianCalendar();
    curCal.setTime(new Date());

    final GregorianCalendar curMonth = new GregorianCalendar(curCal.get(GregorianCalendar.YEAR), curCal
        .get(GregorianCalendar.MONTH), 0, 0, 0);

    final GregorianCalendar exMonth = new GregorianCalendar(exCal.get(GregorianCalendar.YEAR), exCal
        .get(GregorianCalendar.MONTH), 0, 0, 0);

    final GregorianCalendar curDay = new GregorianCalendar(0, 0, curCal.get(GregorianCalendar.DATE), curCal
        .get(GregorianCalendar.HOUR_OF_DAY), curCal.get(GregorianCalendar.MINUTE));

    final GregorianCalendar exDay = new GregorianCalendar(0, 0, exCal.get(GregorianCalendar.DATE), exCal
        .get(GregorianCalendar.HOUR_OF_DAY), exCal.get(GregorianCalendar.MINUTE));

    final GregorianCalendar curDayPlusHour = (GregorianCalendar) curCal.clone();
    curDayPlusHour.add(GregorianCalendar.HOUR_OF_DAY, 1);

    // if lastExecutionTime is more than one hou in the future release the
    // trigger.
    if (curDayPlusHour.getTimeInMillis() < time) {

      // set last execution time
      final GregorianCalendar newExDay = new GregorianCalendar(curCal.get(GregorianCalendar.YEAR), curCal
          .get(GregorianCalendar.MONTH), curCal.get(GregorianCalendar.DATE), exCal.get(GregorianCalendar.HOUR), exCal
          .get(GregorianCalendar.MINUTE));

      this.lastExecutionTime = newExDay.getTimeInMillis();
      return true;

    }

    // has this trigger been activated this month
    if (exMonth.before(curMonth) || first) {

      if (!exDay.after(curDay)) {

        // set last execution time
        final GregorianCalendar newExDay = new GregorianCalendar(curCal.get(GregorianCalendar.YEAR), curCal
            .get(GregorianCalendar.MONTH), curCal.get(GregorianCalendar.DATE), exCal.get(GregorianCalendar.HOUR), exCal
            .get(GregorianCalendar.MINUTE));

        this.lastExecutionTime = newExDay.getTimeInMillis();

        return true;
      }

    }
    return false;
  }

}
