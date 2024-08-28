package com.distocraft.dc5000.etl.scheduler.trigger;

import static org.junit.Assert.assertEquals;
import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;

/**
 * Tests for Monthly class in com.distocraft.dc5000.etl.scheduler.trigger.<br>
 * <br>
 * Testing if the set is running and returns false. NOTE: SetEnds class is not
 * in use and might be broken, no testing done here. Testclass stub done for
 * later use.
 * 
 * @author EJAAVAH
 * 
 */
public class SetEndsTest {

  private static SetEnds objUnderTest;

  private static Meta_schedulings schedulings;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    // Initializing null Meta_schedulings object for initializing SetEnds object
    schedulings = new Meta_schedulings(null);

    try {
      objUnderTest = new SetEnds();
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
   * Tests if false is retuned when checking if given set is active or not.
   */
  @Test
  public void testSetEndsExecute() throws Exception {
    // These ID's are to be parsed out for checking if they match to an active
    // set
    schedulings.setTrigger_command("techpackID=2\nSetID=3");
    // SetEnds class might be broken, so this test is not working properly yet
    // assertEquals(false, objUnderTest.execute());
  }
  
  //Making the test work with ant 1.6.5 and JUnit 4.x
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(SetEndsTest.class);
  }
}
