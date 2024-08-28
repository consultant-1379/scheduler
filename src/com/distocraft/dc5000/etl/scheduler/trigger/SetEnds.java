/*
 * Created on 3.2.2005
 *
 */
package com.distocraft.dc5000.etl.scheduler.trigger;

import java.util.HashMap;

import com.distocraft.dc5000.etl.engine.common.Tags;
import com.distocraft.dc5000.etl.engine.main.ITransferEngineRMI;
import com.distocraft.dc5000.etl.scheduler.EventTrigger;
import com.distocraft.dc5000.etl.scheduler.SchedulerConnect;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * @author savinen
 * 
 */
public class SetEnds extends EventTrigger {

	public boolean execute() throws SchedulerException {
		try {
			update();
			final HashMap tagMap = Tags.GetTagPairs("", "=", this.trigger_command);
			
			final ITransferEngineRMI engine = SchedulerConnect.connectEngine();
			engine.isSetRunning(new Long((String) tagMap.get("techpackID")), new Long((String) tagMap.get("setID"))); // ??
			
			return false; // ??
		} catch (Exception e) {
			throw new SchedulerException("Failed in Setends, ", e);
		}
	}

}
