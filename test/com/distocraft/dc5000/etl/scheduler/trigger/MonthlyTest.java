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
 * Tests for Monthly class in com.distocraft.dc5000.etl.scheduler.trigger.<br>
 * <br>
 * Testing the monthly trigger releasing in different cases.
 * 
 * @author EJAAVAH
 * 
 */
public class MonthlyTest {

  private static Monthly objUnderTest;

  private static Meta_schedulings schedulings;

  private static Timestamp tstmp;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    // Initializing null Meta_schedulings object for initializing Monthly object
    schedulings = new Meta_schedulings(null);

    try {
      objUnderTest = new Monthly();
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
   * Testing monthly trigger releasing. Set is triggered if it has not been done
   * this month, it is triggered for the first time or time is more than an hour
   * from last triggering time. True is returned if trigger is released,
   * otherwise false.
   */
  @Test
  public void testMonthlyExecute() throws Exception {
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
    // null), return value is false because no schedulingdate is set on
    // schedulings properties
    tstmp = null;
    schedulings.setLast_execution_time(tstmp);
    assertEquals(false, objUnderTest.execute());
  }

  // Making the test work with ant 1.6.5 and JUnit 4.x
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(MonthlyTest.class);
  }
}
