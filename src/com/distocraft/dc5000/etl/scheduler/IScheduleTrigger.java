package com.distocraft.dc5000.etl.scheduler;

import com.distocraft.dc5000.etl.engine.main.ITransferEngineRMI;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * @author savinen
 */
public interface IScheduleTrigger {

  void init(Meta_schedulings schedule, ITransferEngineRMI engine) throws SchedulerException;

  /**
   * Return true if execution terms are correct
   */
  boolean execute() throws SchedulerException;

  Meta_schedulings getSchedule();

  ITransferEngineRMI getEngine();

  void cancel();

  boolean isActive();

  long getLastExecutionTime();

  String getName();

}
