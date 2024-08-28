package com.distocraft.dc5000.etl.scheduler.trigger;

import static org.junit.Assert.assertEquals;
import java.sql.Timestamp;

import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;

/**
 * Tests for Once class in com.distocraft.dc5000.etl.scheduler.trigger.<br>
 * <br>
 * Testing the once trigger releasing in different cases.
 * 
 * @author EJAAVAH
 * 
 */
public class OnceTest {

  private static Once objUnderTest;

  private static Meta_schedulings schedulings;

  private static Timestamp tstmp;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    // Meta_schedulings object for setting the status flag
    schedulings = new Meta_schedulings(null);

    try {
      objUnderTest = new Once();
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
   * Testing the triggering of the set once by checking the status flag. If
   * trigger has <i>Executed</i> status then the trigger has been released
   * earlier and will not be done again. True is returned if trigger will be
   * released, otherwise false.
   */
  @Test
  public void testOnceExecute() throws Exception {

    // Set has not been triggered yet (status = null), trigger now
    schedulings.setStatus(null);
    assertEquals(true, objUnderTest.execute());

    // Set has been triggered (status = executed), do not trigger
    schedulings.setStatus("Executed");
    assertEquals(false, objUnderTest.execute());
  }

  // Making the test work with ant 1.6.5 and JUnit 4.x
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(OnceTest.class);
  }
}
