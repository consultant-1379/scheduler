package com.distocraft.dc5000.etl.scheduler.trigger;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.Before;

import org.junit.Test;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * Tests for IntervalDirCheck class in com.distocraft.dc5000.etl.scheduler.trigger.<br>
 * <br>
 * Testing if IntervalDirCheck class triggers a set if defined directory exists or not (based on what is required).
 *
 * @author eromsza
 *
 */
public class IntervalDirCheckTest {

  private IntervalDirCheck objUnderTest;

  private Meta_schedulings schedulings;

  private File testDir;

  private File testFile;

  private final Date regularDate = Calendar.getInstance(TimeZone.getDefault()).getTime();

  @Before
  public void setUp() throws Exception {
      // Meta_schedulings for initializing Interval time
      schedulings = new Meta_schedulings(null);
      schedulings.setInterval_hour(0L);
      schedulings.setInterval_min(15L);
  }

  @After
  public void tearDown() throws Exception {
      removeDirs(testDir);
      objUnderTest = null;
      schedulings = null;
  }

  /**
   * Testing if the set is NOT triggered if the NULL isDirCheck is provided and the NULL directory is provided. False should be returned.
   */
  @Test(expected = SchedulerException.class)
  public void testCheckNull() throws Exception {
      final Boolean isDirCheck = null;
      final String pathsToTest = null;
      // Testing triggering with null value, set should not be triggered (check for emptiness)
      init(isDirCheck, pathsToTest);
      objUnderTest.execute();
  }

  /**
   * Testing if the set is NOT triggered if the NULL isDirCheck is provided. False should be returned.
   */
  @Test(expected = SchedulerException.class)
  public void testCheckIsDirCheckNull() throws Exception {
      final Boolean isDirCheck = null;
      final String pathsToTest = "anyDirectory";
      // Testing triggering with null value, set should not be triggered (check for emptiness)
      init(isDirCheck, pathsToTest);
      objUnderTest.execute();
  }

  /**
   * Testing if the set is NOT triggered if the NULL directory is provided. False should be returned.
   */
  @Test(expected = SchedulerException.class)
  public void testCheckNullDirectory() throws Exception {
      final String pathsToTest = null;
      // Testing triggering with null value, set should not be triggered (check for emptiness)
      init(true, pathsToTest);
      objUnderTest.execute();
  }

  /**
   * Testing if the set is NOT triggered if the non-existing directory is provided. False should be returned.
   */
  @Test(expected = SchedulerException.class)
  public void testCheckNotExistingDirectory() throws Exception {
      final String pathsToTest = "nonExistingDirectory";
      // Testing triggering with null value, set should not be triggered (check for emptiness)
      init(true, pathsToTest);
      objUnderTest.execute();
  }

  /**
   * Testing if the set is triggered if the directory is empty. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryIsEmpty() throws Exception {
      final boolean checkIfDirEmpty = true;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir";
      // Creating a file to be tested if it exists
      testDir = new File(pathsToTest);
      testDir.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest);
      assertEquals(true, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the directory is NOT empty. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryIsNotEmpty() throws Exception {
      final boolean checkIfDirEmpty = false;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir";
      // Creating a file to be tested if it exists
      testDir = new File(pathsToTest);
      testDir.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest);
      assertEquals(true, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the directory is empty and the time is shifted. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryIsEmptyAndMoreThanOneHourInTheFuture() throws Exception {
      final boolean checkIfDirEmpty = true;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir";
      // Creating a file to be tested if it exists
      testDir = new File(pathsToTest);
      testDir.mkdir();

      init(checkIfDirEmpty, pathsToTest, regularDate, 3610000L);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest, regularDate, 3610000L);
      assertEquals(true, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest, regularDate, 3610000L);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the directory is NOT empty and the time is shifted. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryIsNotEmptyAndMoreThanOneHourInTheFuture() throws Exception {
      final boolean checkIfDirEmpty = false;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir";
      // Creating a file to be tested if it exists
      testDir = new File(pathsToTest);
      testDir.mkdir();

      init(checkIfDirEmpty, pathsToTest, regularDate, 3610000L);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest, regularDate, 3610000L);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest, regularDate, 3610000L);
      assertEquals(true, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the directory with the '*' wildcard is empty. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryWithWildcardIsEmpty() throws Exception {
      final boolean checkIfDirEmpty = true;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir/*";
      // Creating a file to be tested if it exists
      testDir = new File(System.getProperty("java.io.tmpdir"), "testDir");
      testDir.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());

      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest);
      assertEquals(true, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the directory with the '*' wildcard is NOT empty. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryWithWildcardIsNotEmpty() throws Exception {
      final boolean checkIfDirEmpty = false;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir/*";
      // Creating a file to be tested if it exists
      testDir = new File(System.getProperty("java.io.tmpdir"), "testDir");
      testDir.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest);
      assertEquals(true, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the nested directory with the '*' wildcard is empty. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryWithWildcardAndNestedDirIsEmpty() throws Exception {
      final boolean checkIfDirEmpty = true;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir/nestedDir/*";
      // Creating a file to be tested if it exists
      testDir = new File(System.getProperty("java.io.tmpdir"), "testDir");
      testDir.mkdir();

      File testDir2 = new File(testDir, "nestedDir");
      testDir2.mkdir();
      testDir2 = new File(testDir2, "ERBS");
      testDir2.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest);
      assertEquals(true, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir2, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the nested directory with the '*' wildcard is empty. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryWithWildcardAndNestedDirIsNotEmpty() throws Exception {
      final boolean checkIfDirEmpty = false;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir/nestedDir/*";
      // Creating a file to be tested if it exists
      testDir = new File(System.getProperty("java.io.tmpdir"), "testDir");
      testDir.mkdir();

      File testDir2 = new File(testDir, "nestedDir");
      testDir2.mkdir();
      testDir2 = new File(testDir2, "ERBS");
      testDir2.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir2, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest);
      assertEquals(true, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the nested directory with the '*' wildcard is empty. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryWithWildcardAndMultipleNestedDirsIsEmpty() throws Exception {
      final boolean checkIfDirEmpty = true;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir/*/ERBS/test, " + System.getProperty("java.io.tmpdir") + "testDir/nestedDir/Cell";
      // Creating a file to be tested if it exists
      testDir = new File(System.getProperty("java.io.tmpdir"), "testDir");
      testDir.mkdir();

      final File testDir2 = new File(testDir, "nestedDir");
      testDir2.mkdir();
      final File testDir3 = new File(testDir2, "ERBS");
      testDir3.mkdir();
      final File testDir4 = new File(testDir2, "Cell");
      testDir4.mkdir();
      final File testDir5 = new File(testDir3, "test");
      testDir5.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest);
      assertEquals(true, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir5, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());
  }

  /**
   * Testing if the set is triggered if the nested directory with the '*' wildcard is empty. True is
   * returned if set is triggered, false otherwise
   */
  @Test
  public void testCheckDirectoryWithWildcardAndMultipleNestedDirsIsNotEmpty() throws Exception {
      final boolean checkIfDirEmpty = false;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir/*/ERBS/test, " + System.getProperty("java.io.tmpdir") + "testDir/nestedDir/Cell";
      // Creating a file to be tested if it exists
      testDir = new File(System.getProperty("java.io.tmpdir"), "testDir");
      testDir.mkdir();

      final File testDir2 = new File(testDir, "nestedDir");
      testDir2.mkdir();
      final File testDir3 = new File(testDir2, "ERBS");
      testDir3.mkdir();
      final File testDir4 = new File(testDir2, "Cell");
      testDir4.mkdir();
      final File testDir5 = new File(testDir3, "test");
      testDir5.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is NOT empty for the defined path in BeforeClass
      assertEquals(false, objUnderTest.execute());

      // Creating a file to be tested
      testFile = new File(testDir5, "testFile.txt");
      final PrintWriter pw = new PrintWriter(new FileWriter(testFile));
      pw.write("testing");
      pw.close();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());

      // Asserting that the set is NOT triggered before the next interval elapses
      assertEquals(false, objUnderTest.execute());
      // Asserting that the set is triggered after the next interval elapses (the init method just simulates it)
      init(checkIfDirEmpty, pathsToTest);
      assertEquals(true, objUnderTest.execute());
  }

  // Making the test work with ant 1.6.5 and JUnit 4.x
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(IntervalDirCheckTest.class);
  }

  /**
   * Testing if the SchedulerException is thrown if the required nested directory with the '*' wildcard is invalid.
   */
  @Test(expected = SchedulerException.class)
  public void testCheckNotExistingDirectoryWithWildcard() throws Exception {
      final boolean checkIfDirEmpty = true;
      final String pathsToTest = System.getProperty("java.io.tmpdir") + "testDir/*/wrong_dir/test";
      // Creating a file to be tested if it exists
      testDir = new File(System.getProperty("java.io.tmpdir"), "testDir");
      testDir.mkdir();

      final File testDir2 = new File(testDir, "nestedDir");
      testDir2.mkdir();
      final File testDir3 = new File(testDir2, "ERBS");
      testDir3.mkdir();

      init(checkIfDirEmpty, pathsToTest);
      // Asserting that the set is triggered when directory is empty for the defined path in BeforeClass
      assertEquals(true, objUnderTest.execute());
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Remove recursively tempDir directory structure.
   *
   * @param dirOrFile the file or the directory
   */
  private void removeDirs(final File dirOrFile) {
      if (dirOrFile != null) {
          if (dirOrFile.isDirectory() && dirOrFile.list().length > 0) {
              for (String dirName : dirOrFile.list()) {
                  removeDirs(new File(dirOrFile.getAbsolutePath() + File.separator + dirName));
              }
          }
          dirOrFile.delete();
      }
  }

  /**
   * Initialise the tested object.
   *
   * @param pathsToTest the paths to test emptiness
   */
  private void init(final Boolean isDirEmpty, final String pathsToTest) {
      init(isDirEmpty, pathsToTest, Calendar.getInstance(TimeZone.getDefault()).getTime(), -910000L);
  }

  /**
   * Initialise the tested object.
   *
   * @param pathsToTest the paths to test emptiness
   */
  private void init(final Boolean isDirEmpty, final String pathsToTest, final Date date, final long timeShift) {
      try {
          String command = ";";

          if (isDirEmpty != null) {
              command = isDirEmpty + command;
          }
          if (pathsToTest != null) {
              command = command + pathsToTest;
          }

          schedulings.setTrigger_command(command);

          schedulings.setLast_execution_time(new Timestamp(date.getTime() + timeShift));
          objUnderTest = new IntervalDirCheck();
          objUnderTest.init(schedulings, null);
      } catch (Exception e) {
          e.printStackTrace();
      }
  }

}
