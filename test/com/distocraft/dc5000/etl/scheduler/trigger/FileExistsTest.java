package com.distocraft.dc5000.etl.scheduler.trigger;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;

/**
 * Tests for FileExists class in com.distocraft.dc5000.etl.scheduler.trigger.<br>
 * <br>
 * Testing if FileExists class triggers a set if defined file exists or not.
 * 
 * @author EJAAVAH
 * 
 */
public class FileExistsTest {

  private static FileExists objUnderTest;

  private static Meta_schedulings schedulings;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    // Creating a file to be tested if it exists
    final File testFile = new File(System.getProperty("java.io.tmpdir"), "testFile.txt");
    testFile.deleteOnExit();
    PrintWriter pw = new PrintWriter(new FileWriter(testFile));
    pw.write("testing");
    pw.close();

    // Meta_schedulings for defining filepath to be looked for
    schedulings = new Meta_schedulings(null);
    schedulings.setTrigger_command(testFile.getPath());

    try {
      objUnderTest = new FileExists();
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
   * Testing if the set is triggered if the file exists/doesn't exist. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testFileExistsExecute() throws Exception {
    // Asserting that the file is triggered when file exists in the path defined
    // in BeforeClass
    assertEquals(true, objUnderTest.execute());

    // Setting not existing filepath and asserting that set is not triggered
    schedulings.setTrigger_command("notExistingPath");
    assertEquals(false, objUnderTest.execute());

    // Testing triggering with null value, set should not be triggered
    schedulings.setTrigger_command(null);
    assertEquals(false, objUnderTest.execute());
  }

  // Making the test work with ant 1.6.5 and JUnit 4.x
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(FileExistsTest.class);
  }
}
