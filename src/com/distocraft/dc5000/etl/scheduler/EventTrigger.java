package com.distocraft.dc5000.etl.scheduler;

import com.distocraft.dc5000.etl.engine.main.ITransferEngineRMI;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * @author savinen
 * 
 */
public class EventTrigger implements IScheduleTrigger {

  protected Meta_schedulings schedule;

  protected ITransferEngineRMI engine;

  protected boolean active = true;

  protected String os_command;

  protected String trigger_command;

  protected long lastExecutionTime;

  protected String name;

  /**
   * Reads information from db
   *
   * @throws Exception
   */
  public void update() throws SchedulerException {

    if (this.schedule.getOs_command() != null){
      this.os_command = this.schedule.getOs_command();
    }
    
    if (this.schedule.getTrigger_command() != null){
      this.trigger_command = this.schedule.getTrigger_command();
    }

    if (this.schedule.getLast_execution_time() != null){
      this.lastExecutionTime = this.schedule.getLast_execution_time().getTime();
    }

    if (this.schedule.getOs_command() != null){
      this.name = this.schedule.getName();
    }

    if (this.schedule.getHold_flag() != null && schedule.getHold_flag().equalsIgnoreCase("n")){
      this.active = true;
    }
    else{
      this.active = false;
    }

  }

  /**
   * Inits this class
   */
  public void init(final Meta_schedulings schedule, final ITransferEngineRMI engine) throws SchedulerException {

    this.schedule = schedule;
    this.engine = engine;
    
    this.update();

  }

  /**
   * This is extended elswhere
   */
  public boolean execute() throws SchedulerException {
    return true;
  }

  /**
   * Returns schedule
   */
  public Meta_schedulings getSchedule() {
    return this.schedule;
  }

  /**
   * Return engine
   */
  public ITransferEngineRMI getEngine() {
    return this.engine;
  }

  public void cancel() {

  }

  /**
   * Returns true if trigger is not on hold
   */
  public boolean isActive() {
    return this.active;
  }

  /**
   * Returns the last execution time
   */
  public long getLastExecutionTime() {
    return this.lastExecutionTime;
  }

  public String getName() {
    return this.name;
  }

}
