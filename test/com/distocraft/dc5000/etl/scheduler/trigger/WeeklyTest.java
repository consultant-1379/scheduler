package com.distocraft.dc5000.etl.scheduler.trigger;

import static org.junit.Assert.assertEquals;
import java.sql.Timestamp;
import java.util.Date;

import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;

/**
 * Tests for Weekly class in com.distocraft.dc5000.etl.scheduler.trigger.<br>
 * <br>
 * Testing weekly triggering.
 * 
 * @author EJAAVAH
 * 
 */
public class WeeklyTest {

  private static Weekly objUnderTest;

  private static Meta_schedulings schedulings;

  private static Timestamp tstmp;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    // Setting weekday flags to enabled so that the tests can be run on any day
    schedulings = new Meta_schedulings(null);
    schedulings.setMon_flag("y");
    schedulings.setTue_flag("y");
    schedulings.setWed_flag("y");
    schedulings.setThu_flag("y");
    schedulings.setFri_flag("y");
    schedulings.setSat_flag("y");
    schedulings.setSun_flag("y");

    try {
      objUnderTest = new Weekly();
      objUnderTest.init(schedulings, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    schedulings = null;
    tstmp = null;
    objUnderTest = null;
  }

  /**
   * Testing weekly triggering. Set is triggered if it is being triggered for
   * the first time or time is more than an hour from last triggering time.
   * Triggering days can be modified by enabling and disabling weekday flags in
   * schedulings. True is returned if set is triggered, otherwise false.
   */
  @Test
  public void testWeeklyExecute() throws Exception {
    // Testing if the trigger is relesed if the last execution time is more than
    // an hour in the future
    tstmp = new Timestamp(new Date().getTime() + 3600050);
    schedulings.setLast_execution_time(tstmp);
    assertEquals(true, objUnderTest.execute());

    // Trigger should not be released when last execute time is less than an
    // hour in the future
    tstmp = new Timestamp(new Date().getTime() + 3599950);
    schedulings.setLast_execution_time(tstmp);
    assertEquals(false, objUnderTest.execute());

    // Testing if the trigger is set on the first execution (last triggered =
    // null)
    tstmp = null;
    schedulings.setLast_execution_time(tstmp);
    assertEquals(true, objUnderTest.execute());
  }

  // Making the test work with ant 1.6.5 and JUnit 4.x
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(WeeklyTest.class);
  }
}
