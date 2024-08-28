package com.distocraft.dc5000.etl.scheduler;

import java.util.GregorianCalendar;

import com.distocraft.dc5000.etl.engine.main.ITransferEngineRMI;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * @author savinen
 */
public class TimeTrigger implements IScheduleTrigger {

  protected Meta_schedulings schedule;

  protected ITransferEngineRMI engine;

  protected boolean active = true;

  protected int year;

  protected int month;

  protected int day;

  protected int hour;

  protected int minute;

  protected int intervalHour;

  protected int intervalMinute;

  protected long lastExecutionTime;
  
  protected long executionTime;

  protected String status;

  protected String name;

  protected String OSCommand;

  public void update() throws SchedulerException {

    if (this.schedule.getInterval_hour() != null){
      this.intervalHour = this.schedule.getInterval_hour().intValue();
    }
    if (this.schedule.getInterval_min() != null){
      this.intervalMinute = this.schedule.getInterval_min().intValue();
    }

    if (this.schedule.getScheduling_year() != null){
      this.year = this.schedule.getScheduling_year().intValue();
    }
    if (this.schedule.getScheduling_month() != null){
      this.month = this.schedule.getScheduling_month().intValue();
    }
    if (this.schedule.getScheduling_day() != null){
      this.day = this.schedule.getScheduling_day().intValue();
    }
    if (this.schedule.getScheduling_hour() != null){
      this.hour = this.schedule.getScheduling_hour().intValue();
    }
    if (this.schedule.getScheduling_min() != null){
      this.minute = this.schedule.getScheduling_min().intValue();
    }

    if (this.schedule.getOs_command() != null){
      this.OSCommand = this.schedule.getOs_command();
    }

    if (this.schedule.getStatus() != null){
      this.status = this.schedule.getStatus();
    }

    if (this.schedule.getName() != null){
      this.name = this.schedule.getName();
    }

    if (this.schedule.getLast_exec_time_ms() != null && this.schedule.getLast_exec_time_ms().intValue() > 0) {
      this.lastExecutionTime = this.schedule.getLast_exec_time_ms().longValue();
    } else if (this.schedule.getLast_execution_time() != null) {
      this.lastExecutionTime = this.schedule.getLast_execution_time().getTime();
    } else {
      this.lastExecutionTime = 0;
    }

    final GregorianCalendar cal = new GregorianCalendar();
    cal.set(year, month, day, hour, minute);
    this.executionTime = cal.getTimeInMillis();

    if (this.schedule.getHold_flag() != null && schedule.getHold_flag().equalsIgnoreCase("n")) {
      this.active = true;
    } else {
      this.active = false;
    }

  }

  public void init(final Meta_schedulings schedule, final ITransferEngineRMI engine) throws SchedulerException {

    this.schedule = schedule;
    this.engine = engine;
    this.update();

  }

  public boolean execute() throws SchedulerException {
    return true;
  }

  public Meta_schedulings getSchedule() {
    return this.schedule;
  }

  public ITransferEngineRMI getEngine() {
    return this.engine;
  }

  public void cancel() {

  }

  public boolean isActive() {
    return this.active;
  }

  public long getLastExecutionTime() {
    return this.lastExecutionTime;
  }

  public String getName() {
    return this.name;
  }

}
