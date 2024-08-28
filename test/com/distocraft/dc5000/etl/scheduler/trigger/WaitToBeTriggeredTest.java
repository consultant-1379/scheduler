package com.distocraft.dc5000.etl.scheduler.trigger;

import static org.junit.Assert.assertEquals;
import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;

/**
 * Tests for WaitToBeTriggered class in
 * com.distocraft.dc5000.etl.scheduler.trigger.<br>
 * <br>
 * Testing if set is put on hold.
 * 
 * @author EJAAVAH
 * 
 */
public class WaitToBeTriggeredTest {

  private static WaitToBeTriggered objUnderTest;

  private static Meta_schedulings schedulings;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    // Initializing null Meta_schedulings object for initializing
    // WaitToBeTriggered object
    schedulings = new Meta_schedulings(null);

    try {
      objUnderTest = new WaitToBeTriggered();
      objUnderTest.init(schedulings, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    schedulings = null;
    objUnderTest = null;
  }

  /**
   * WaitToBeTriggered can be called when set is put on hold and will always
   * return false, meaning that trigger has not been triggered.
   */
  @Test
  public void testWaitToBeTriggeredExecute() throws Exception {
    // Testing if false is returned
    assertEquals(false, objUnderTest.execute());
  }

  // Making the test work with ant 1.6.5 and JUnit 4.x
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(WaitToBeTriggeredTest.class);
  }
}
