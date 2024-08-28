package com.distocraft.dc5000.etl.scheduler;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Vector;
import junit.framework.JUnit4TestAdapter;
import org.dbunit.Assertion;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ssc.rockfactory.RockFactory;
import com.distocraft.dc5000.etl.engine.main.ITransferEngineRMI;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;
import com.distocraft.dc5000.etl.scheduler.trigger.Once;

/**
 * Tests for SchedulerThread class in com.distocraft.dc5000.etl.scheduler.<br>
 * <br>
 * Testing scheduler thread creating, starting and canceling. Threads are
 * created from META_SCHEDULES table, and once created they are added to the
 * thread list. Threads from this list can then be started (executing the
 * scheduling) or canceled (removing the threads from the list).
 *
 * @author EJAAVAH
 *
 */
public class SchedulerThreadTest {

  private static SchedulerThread objUnderTest;

  private static Connection con = null;

  private static Statement stmt;

  private static Method makeThreads;

  private static Method startThreads;

  private static Method triggerSet;

  private static Method createScheduleTrigger;

  private static Method initRock;

  private static Method executionFailed;

  private static Method getDescription;

  private static Method getTechpackName;

  private static Field triggerThreadList;

  private static Field stopped;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    // Setting up database tables to be used in testing
    try {
      Class.forName("org.hsqldb.jdbcDriver").newInstance();
      con = DriverManager.getConnection("jdbc:hsqldb:mem:testdb", "sa", "");
    } catch (Exception e) {
      e.printStackTrace();
    }
    stmt = con.createStatement();
    stmt.execute("CREATE TABLE META_COLLECTION_SETS (COLLECTION_SET_ID VARCHAR(100),COLLECTION_SET_NAME VARCHAR(100), "
        + "DESCRIPTION VARCHAR(100), VERSION_NUMBER VARCHAR(100), ENABLED_FLAG VARCHAR(100), TYPE VARCHAR(100))");

    stmt.execute("CREATE TABLE META_SCHEDULINGS (VERSION_NUMBER VARCHAR(100), ID BIGINT, EXECUTION_TYPE VARCHAR(100), "
        + "OS_COMMAND VARCHAR(100), SCHEDULING_MONTH BIGINT, SCHEDULING_DAY BIGINT, SCHEDULING_HOUR BIGINT, "
        + "SCHEDULING_MIN BIGINT, COLLECTION_SET_ID BIGINT, COLLECTION_ID BIGINT, MON_FLAG VARCHAR(100), "
        + " TUE_FLAG VARCHAR(100), WED_FLAG VARCHAR(100), THU_FLAG VARCHAR(100), FRI_FLAG VARCHAR(100), "
        + "SAT_FLAG VARCHAR(100), SUN_FLAG VARCHAR(100), STATUS VARCHAR(100), LAST_EXECUTION_TIME TIMESTAMP, "
        + "INTERVAL_HOUR BIGINT, INTERVAL_MIN BIGINT, NAME VARCHAR(100), HOLD_FLAG VARCHAR(100), PRIORITY BIGINT, "
        + "SCHEDULING_YEAR BIGINT, TRIGGER_COMMAND VARCHAR(100), LAST_EXEC_TIME_MS BIGINT)");

    stmt.execute("CREATE TABLE META_COLLECTIONS (COLLECTION_ID BIGINT, COLLECTION_NAME VARCHAR(100), "
        + "COLLECTION VARCHAR(100), MAIL_ERROR_ADDR VARCHAR(100), MAIL_FAIL_ADDR VARCHAR(100), "
        + "MAIL_BUG_ADDR VARCHAR(100), MAX_ERRORS BIGINT, MAX_FK_ERRORS BIGINT, MAX_COL_LIMIT_ERRORS BIGINT, "
        + "CHECK_FK_ERROR_FLAG VARCHAR(100), CHECK_COL_LIMITS_FLAG VARCHAR(100), LAST_TRANSFER_DATE TIMESTAMP, "
        + "VERSION_NUMBER VARCHAR(100), COLLECTION_SET_ID BIGINT, USE_BATCH_ID VARCHAR(100), PRIORITY BIGINT, "
        + "QUEUE_TIME_LIMIT BIGINT, ENABLED_FLAG VARCHAR(100), SETTYPE VARCHAR(100), FOLDABLE_FLAG VARCHAR(100), "
        + "MEASTYPE VARCHAR(100), HOLD_FLAG VARCHAR(100), SCHEDULING_INFO VARCHAR(100))");

    stmt.execute("CREATE TABLE META_TRANSFER_ACTIONS (VERSION_NUMBER VARCHAR(100), TRANSFER_ACTION_ID BIGINT, "
        + "COLLECTION_ID BIGINT, COLLECTION_SET_ID BIGINT, ACTION_TYPE VARCHAR(100), TRANSFER_ACTION_NAME VARCHAR(100), "
        + "ORDER_BY_NO BIGINT, DESCRIPTION VARCHAR(100), ENABLED_FLAG VARCHAR(100), CONNECTION_ID BIGINT, "
        + "WHERE_CLAUSE_02 VARCHAR(100), WHERE_CLAUSE_03 VARCHAR(100), ACTION_CONTENTS_03 VARCHAR(100), "
        + "ACTION_CONTENTS_02 VARCHAR(100), ACTION_CONTENTS_01 VARCHAR(100), WHERE_CLAUSE_01 VARCHAR(100))");

    // Initializing the RMI object to be used by ThreadScheduler
    ITransferEngineRMIStub transferEngine = new ITransferEngineRMIStub("TransferEngine", 1200);

    // Reflecting methods that are used only in single test
    try {
      objUnderTest = new SchedulerThread(1L, 1, "//localhost:1200/TransferEngine", 1L, "jdbc:hsqldb:mem:testdb",
          "org.hsqldb.jdbcDriver", "sa", "");
      Class ST = objUnderTest.getClass();
      startThreads = ST.getDeclaredMethod("startThreads", new Class[] {});
      startThreads.setAccessible(true);
      triggerSet = ST.getDeclaredMethod("triggerSet", new Class[] { IScheduleTrigger.class, Meta_schedulings.class,
          ITransferEngineRMI.class, String.class, String.class });
      triggerSet.setAccessible(true);
      createScheduleTrigger = ST.getDeclaredMethod("createScheduleTrigger", new Class[] { Meta_schedulings.class,
          ITransferEngineRMI.class });
      createScheduleTrigger.setAccessible(true);
      executionFailed = ST.getDeclaredMethod("executionFailed", new Class[] { IScheduleTrigger.class,
          Meta_schedulings.class });
      executionFailed.setAccessible(true);
      getDescription = ST.getDeclaredMethod("getDesc", new Class[] { Meta_schedulings.class });
      getDescription.setAccessible(true);
      getTechpackName = ST.getDeclaredMethod("getTechpackName", new Class[] { Meta_schedulings.class });
      getTechpackName.setAccessible(true);
      triggerThreadList = ST.getDeclaredField("triggerThreadList");
      triggerThreadList.setAccessible(true);
      stopped = ST.getDeclaredField("stopped");
      stopped.setAccessible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    stmt.execute("DROP TABLE META_SCHEDULINGS");
    stmt.execute("DROP TABLE META_COLLECTIONS");
    stmt.execute("DROP TABLE META_TRANSFER_ACTIONS");
    stmt.execute("DROP TABLE META_COLLECTION_SETS");
    con = null;
    objUnderTest = null;
  }

  @Before
  public void SetUpBeforeTest() throws Exception {
    // Initializing often used objects/methods before every test to avoid mix
    // ups
    try {
      objUnderTest = new SchedulerThread(1L, 1, "//localhost:1200/Engine", 1L, "jdbc:hsqldb:mem:testdb",
          "org.hsqldb.jdbcDriver", "sa", "");
      Class ST = objUnderTest.getClass();
      makeThreads = ST.getDeclaredMethod("makeThreads", new Class[] {});
      makeThreads.setAccessible(true);
      initRock = ST.getDeclaredMethod("initRock", new Class[] {});
      initRock.setAccessible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @After
  public void CleanUpAfterTest() throws Exception {
    // Clearing all previous rows from tables used in latest test
    stmt.execute("DELETE FROM META_COLLECTION_SETS");
    stmt.execute("DELETE FROM META_SCHEDULINGS");
    stmt.execute("DELETE FROM META_COLLECTIONS");
    initRock = null;
    makeThreads = null;
    objUnderTest = null;
  }

	private File getFile(final String name) throws Exception {
		final URL url = ClassLoader.getSystemResource("XMLFiles");
		if(url == null){
			throw new FileNotFoundException("XMLFiles");
		}
		final File xmlBase = new File(url.toURI());
		final String xmlFile = xmlBase.getAbsolutePath() + "/"+name;
		return new File(xmlFile);
	}
  /**
   * Testing thread creation by inserting collection sets with schedules in them
   * and asserting that the threads are put into vector object.
   */
  @Test
  public void testMakeThreads() throws Exception {

    // Inserting test data into tables
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing scheduler thread creating', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('1', 'testset', 'testing scheduler thread creating from another collection set', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('2', 'testset', 'No threads are created from this set, flag is disabled', '1.0', 'N', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 1, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule1', 'N', 1, 2008, 'triggercommand',0)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.2', 1, 'weekly', 'oscommand', 2, 8, 3, 4, 0, 1, 'N', 'Y', 'N', 'Y', 'Y', 'N', 'N', '',"
        + " '1999-12-04 00:00:00.0', NULL, 1, 'testschedule2', 'N', 1, 2001, 'triggercommand',9)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('0.9', 2, 'interval', 'oscommand', 0, 4, 1, 1, 1, 1, 'N', 'N', 'N', 'N', 'N', 'Y', 'N', '',"
        + " '2000-10-02 00:00:00.0', NULL, 1, 'testschedule3', 'N', 1, 2006, 'triggercommand',2)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 4, 'fileexists', 'oscommand', 2, 0, 0, 2, 2, 0, 'Y', 'N', 'N', 'N', 'Y', 'N', 'N', 'inactive',"
        + " '2000-10-02 00:00:00.0', NULL, 1, 'testschedule4', 'N', 1, 2006, 'triggercommand',2)");

    try {
      makeThreads.invoke(objUnderTest, new Object[] {});
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Reflecting triggerThreadList from SchedulerThread and casting it to
    // Vector to be able to compare it to expected results
    Vector actual = (Vector) triggerThreadList.get(objUnderTest);

    // Asserting the size of the vector
    assertEquals(3, actual.size());

    // Asserting names of schedules in vector
    for (int i = 0; i < actual.size(); i++) {
      IScheduleTrigger sTrigger = (IScheduleTrigger) actual.elementAt(i);
      assertEquals("testschedule" + (i + 1), sTrigger.getName());
    }
  }

  /**
   * Testing thread canceling by creating a schedule, checking it exists and
   * then calling cancel() method. This should cause removal of all elements in
   * the threadlist vector and stopped flag to be changed from <i>FALSE</i> to
   * <i>TRUE</i>.
   */
  @Test
  public void testCancel() throws Exception {

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing thread canceling', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 1, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule1', 'N', 1, 2008, 'triggercommand',0)");

    try {
      // Creating schedules to be canceled
      makeThreads.invoke(objUnderTest, new Object[] {});
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Reflecting triggerThreadList from SchedulerThread and casting it to
    // Vector to be able to compare it to expected results
    Vector actual = (Vector) triggerThreadList.get(objUnderTest);
    assertEquals(1, actual.size());
    assertEquals(false, stopped.getBoolean(objUnderTest));

    // Calling the cancel method which should change the flag and remove all
    // elements from the threadlist vector
    objUnderTest.cancel();
    actual = (Vector) triggerThreadList.get(objUnderTest);
    assertEquals(0, actual.size());
    assertEquals(true, stopped.getBoolean(objUnderTest));
  }

  /**
   * Testing thread starting according to its name. Thread is started if it is
   * found from the thread list and it is not on hold. True is returned if
   * thread is started, false otherwise.
   */
  @Test
  public void testStartThread() throws Exception {

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing thread starting by its name', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule1', 'N', 1, 2008, 'triggercommand',0)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule2', 'Y', 1, 2008, 'triggercommand',0)");
    stmt.executeUpdate("INSERT INTO META_COLLECTIONS VALUES"
        + "(0, 'testcollection', 'collection', 'mailerroraddrs', 'mailfailaddrs', 'mailbugaddrs', 1, 1, 1, 'Y',"
        + " 'Y', '2008-01-01 00:00:00.0', '1.0', 0, '0', 1, 1, 'Y', 'A', 'Y', 'meastype', 'Y', 'schedulinginfo')");

    try {
      // Creating schedules to be started
      makeThreads.invoke(objUnderTest, new Object[] {});
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Asserting that not existing schedule returns false
    assertEquals(false, objUnderTest.startThread("NotExistingSchedule", "triggercommand"));

    // Asserting that started thread returns true
    assertEquals(true, objUnderTest.startThread("testschedule1", "triggercommand"));

    // Asserting that thread on hold is not started
    assertEquals(false, objUnderTest.startThread("testschedule2", "triggercommand"));
  }

  /**
   * Testing starting threads. All threads in schedulings will be listed and
   * those with disabled hold flags will be executed. Asserting that all rows
   * exists in schedulings table and those with disabled hold flags are executed
   * (changes made to columns).
   */
  @Test
  public void testStartThreads() throws Exception {

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing starting all threads', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 5, 2, 0, 0, 'N', 'Y', 'Y', 'Y', 'N', 'Y', 'Y', 'status',"
        + " '2008-05-05 00:00:00.0', 2, 4, 'testschedule1', 'N', 5, 2007, 'triggercommand', 0)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.2', 1, 'interval', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule2', 'Y', 1, 2011, 'triggercommand', 0)");
    stmt.executeUpdate("INSERT INTO META_COLLECTIONS VALUES"
        + "(0, 'testcollection', 'collection', 'mailerroraddrs', 'mailfailaddrs', 'mailbugaddrs', 1, 1, 1, 'Y',"
        + " 'Y', '2008-01-01 00:00:00.0', '1.0', 0, '0', 1, 1, 'Y', 'A', 'Y', 'meastype', 'Y', 'schedulinginfo')");

    try {
      // Creating schedules to be started
      makeThreads.invoke(objUnderTest, new Object[] {});
    } catch (Exception e) {
      e.printStackTrace();
    }

    startThreads.invoke(objUnderTest, new Object[] {});

    IDataSet actualDataSet = new DatabaseConnection(con).createDataSet();
    ITable actualTable = actualDataSet.getTable("META_SCHEDULINGS");

   /* IDataSet expectedDataSet = new FlatXmlDataSet(new File(
        "test/XMLFiles/com.distocraft.dc5000.etl.scheduler_testStartThreads/Expected.xml"));*/
    IDataSet expectedDataSet = new FlatXmlDataSet(getFile(
    "com.distocraft.dc5000.etl.scheduler_testStartThreads/Expected.xml"));
    ITable expectedTable = expectedDataSet.getTable("META_SCHEDULINGS");

    // Filtering out few time related rows in order to make the assertion work
    ITable filteredTable = DefaultColumnFilter.includedColumnsTable(actualTable, expectedTable.getTableMetaData()
        .getColumns());

    Assertion.assertEquals(expectedTable, filteredTable);

    // Asserting the time related rows
    Date curDate = new Date();
    Timestamp ts = new Timestamp(108, 0, 1, 0, 0, 0, 0);
    assertEquals(ts, actualTable.getValue(0, "LAST_EXECUTION_TIME"));
    ts = new Timestamp(108, 4, 5, 0, 0, 0, 0);
    assertEquals(ts, actualTable.getValue(1, "LAST_EXECUTION_TIME"));
  }

  /**
   * Testing set triggering. Trigger, engine and scheduling data is given as
   * parameters and actions are made according to this information. Set is
   * triggered which causes certain changes to the META_SCHEDULINGS table.
   */
  @Test
  public void testTriggerSet() throws Exception {

    // Initializing necessary objects for testing
    RockFactory rf = new RockFactory("jdbc:hsqldb:mem:testdb", "sa", "", "org.hsqldb.jdbcDriver", "con", true);
    IScheduleTrigger sTrigger = new Once();
    ITransferEngineRMIStub engine = new TransferEngineStub();
    Meta_schedulings ms = new Meta_schedulings(rf);
    initRock.invoke(objUnderTest, new Object[] {});

    // Testing if exception is thrown when trying to set a trigger with empty
    // tables
    try {
      triggerSet.invoke(objUnderTest, new Object[] { sTrigger, ms, engine, "testCommand", "testTechpak" });
      fail("Exception expected - No data in tables");
    } catch (Exception e) {
      // Test passed - Exception thrown
    }

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing triggering sets', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule', 'N', 1, 2008, 'triggercommand',0)");
    stmt.executeUpdate("INSERT INTO META_COLLECTIONS VALUES"
        + "(0, 'TestSet', 'collection', 'mailerroraddrs', 'mailfailaddrs', 'mailbugaddrs', 1, 1, 1, 'Y',"
        + " 'Y', '2008-01-01 00:00:00.0', '1.0', 0, '0', 1, 1, 'Y', 'A', 'Y', 'meastype', 'Y', 'schedulinginfo')");

    // Testing if set is executed and changes to database are made (last
    // execution time and status are changed)
    triggerSet.invoke(objUnderTest, new Object[] { sTrigger, ms, engine, "testCommand", "testTechpak" });

    IDataSet actualDataSet = new DatabaseConnection(con).createDataSet();
    ITable actualTable = actualDataSet.getTable("META_SCHEDULINGS");

    IDataSet expectedDataSet = new FlatXmlDataSet(getFile(
        "com.distocraft.dc5000.etl.scheduler_testTriggerSet/Expected.xml"));
    ITable expectedTable = expectedDataSet.getTable("META_SCHEDULINGS");
    System.out.println("\n expectedTable.getRowCount"+expectedTable.getRowCount());
    System.out.println("\n actualTable.getRowCount"+actualTable.getRowCount());
    int i=0;
  	  System.out.println("Values for i="+i);
  	  System.out.println("==========================");
  	  System.out.println(expectedTable.getValue(i, "VERSION_NUMBER")+"---"+actualTable.getValue(i, "VERSION_NUMBER"));
  	  System.out.println(expectedTable.getValue(i, "ID")+"---"+actualTable.getValue(i, "ID"));
  	  System.out.println(expectedTable.getValue(i, "EXECUTION_TYPE")+"---"+actualTable.getValue(i, "EXECUTION_TYPE"));
  	  System.out.println(expectedTable.getValue(i, "OS_COMMAND")+"---"+actualTable.getValue(i, "OS_COMMAND"));
  	  System.out.println(expectedTable.getValue(i, "SCHEDULING_MONTH")+"---"+actualTable.getValue(i, "SCHEDULING_MONTH"));
  	  System.out.println(expectedTable.getValue(i, "SCHEDULING_DAY")+"---"+actualTable.getValue(i, "SCHEDULING_DAY"));
  	  System.out.println(expectedTable.getValue(i, "SCHEDULING_HOUR")+"---"+actualTable.getValue(i, "SCHEDULING_HOUR"));
  	  System.out.println(expectedTable.getValue(i, "SCHEDULING_MIN")+"---"+actualTable.getValue(i, "SCHEDULING_MIN"));
  	  System.out.println(expectedTable.getValue(i, "COLLECTION_SET_ID")+"---"+actualTable.getValue(i, "COLLECTION_SET_ID"));
  	  System.out.println(expectedTable.getValue(i, "COLLECTION_ID")+"---"+actualTable.getValue(i, "COLLECTION_ID"));
  	System.out.println(expectedTable.getValue(i, "MON_FLAG")+"---"+actualTable.getValue(i, "MON_FLAG"));
  	System.out.println(expectedTable.getValue(i, "TUE_FLAG")+"---"+actualTable.getValue(i, "TUE_FLAG"));
  	System.out.println(expectedTable.getValue(i, "WED_FLAG")+"---"+actualTable.getValue(i, "WED_FLAG"));
  	System.out.println(expectedTable.getValue(i, "THU_FLAG")+"---"+actualTable.getValue(i, "THU_FLAG"));
  	System.out.println(expectedTable.getValue(i, "FRI_FLAG")+"---"+actualTable.getValue(i, "FRI_FLAG"));
  	System.out.println(expectedTable.getValue(i, "SAT_FLAG")+"---"+actualTable.getValue(i, "SAT_FLAG"));
  	System.out.println(expectedTable.getValue(i, "SUN_FLAG")+"---"+actualTable.getValue(i, "SUN_FLAG"));
  	System.out.println(expectedTable.getValue(i, "STATUS")+"---"+actualTable.getValue(i, "STATUS"));
  	System.out.println(expectedTable.getValue(i, "LAST_EXECUTION_TIME")+"---"+actualTable.getValue(i, "LAST_EXECUTION_TIME"));
  	System.out.println(expectedTable.getValue(i, "INTERVAL_HOUR")+"---"+actualTable.getValue(i, "INTERVAL_HOUR"));
  	System.out.println(expectedTable.getValue(i, "INTERVAL_MIN")+"---"+actualTable.getValue(i, "INTERVAL_MIN"));
  	System.out.println(expectedTable.getValue(i, "NAME")+"---"+actualTable.getValue(i, "NAME"));
  	System.out.println(expectedTable.getValue(i, "HOLD_FLAG")+"---"+actualTable.getValue(i, "HOLD_FLAG"));
  	System.out.println(expectedTable.getValue(i, "PRIORITY")+"---"+actualTable.getValue(i, "PRIORITY"));
  	System.out.println(expectedTable.getValue(i, "SCHEDULING_YEAR")+"---"+actualTable.getValue(i, "SCHEDULING_YEAR"));
  	System.out.println(expectedTable.getValue(i, "TRIGGER_COMMAND")+"---"+actualTable.getValue(i, "TRIGGER_COMMAND"));
  	System.out.println(expectedTable.getValue(i, "LAST_EXEC_TIME_MS")+"---"+actualTable.getValue(i, "LAST_EXEC_TIME_MS"));
  	 System.out.println("============================");


    Assertion.assertEquals(expectedTable, actualTable);

  }

  /**
   * Testing object creation out of triggering data. Meta_schedulings and engine
   * objects are given as parameters and createScheduleTrigger() method should
   * return scheduletrigger object of given type.
   */
  @Test
  public void testCreateScheduleTrigger() throws Exception {

    RockFactory rf = new RockFactory("jdbc:hsqldb:mem:testdb", "sa", "", "org.hsqldb.jdbcDriver", "con", true);
    ITransferEngineRMIStub engine = new TransferEngineStub();
    Meta_schedulings ms = new Meta_schedulings(rf);

    // Testing if exception is thrown when trying to create trigger with empty
    // schedule object
    try {
      createScheduleTrigger.invoke(objUnderTest, new Object[] { ms, engine });
      fail("Exception expected - triggerSet was called with null parameters");
    } catch (Exception e) {
      // Test passed - Exception thrown
    }

    // Testing if not existing execution type causes null to be returned
    ms.setExecution_type("NOT_EXISTING");
    assertEquals(null, createScheduleTrigger.invoke(objUnderTest, new Object[] { ms, engine }));

    // Testing if onStartup named execution type returns null
    ms.setExecution_type("onStartup");
    assertEquals(null, createScheduleTrigger.invoke(objUnderTest, new Object[] { ms, engine }));

    // Testing generic input by setting execution type to weekly which should
    // return weekly trigger object - asserting that the name and execution type
    // are the same
    // same
    ms.setExecution_type("weekly");
    ms.setName("testweekly");
    IScheduleTrigger ist = (IScheduleTrigger) createScheduleTrigger.invoke(objUnderTest, new Object[] { ms, engine });
    assertEquals("testweekly", ist.getName());
    assertEquals("weekly", ist.getSchedule().getExecution_type());

    // Test for timeDirCheck trigger creation
    ms.setExecution_type("timeDirCheck");
    ms.setName("timeDirCheck");
    ist = (IScheduleTrigger) createScheduleTrigger.invoke(objUnderTest, new Object[] { ms, engine });
    assertEquals("timeDirCheck", ist.getName());
    assertEquals("timeDirCheck", ist.getSchedule().getExecution_type());
  }

  /**
   * Testing reporting of failed executions. Status and execution time fields
   * are changed accordingly in META_SCHEDULINGS when executionFailed() method
   * is called.
   */
  @Test
  public void testExecutionFailed() throws Exception {

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule', 'N', 1, 2008, 'triggercommand',0)");

    RockFactory rf = new RockFactory("jdbc:hsqldb:mem:testdb", "sa", "", "org.hsqldb.jdbcDriver", "con", true);
    IScheduleTrigger sTrigger = new Once();
    Meta_schedulings ms = new Meta_schedulings(rf);

    executionFailed.invoke(objUnderTest, new Object[] { sTrigger, ms });

    IDataSet actualDataSet = new DatabaseConnection(con).createDataSet();
    ITable actualTable = actualDataSet.getTable("META_SCHEDULINGS");

    IDataSet expectedDataSet = new FlatXmlDataSet(getFile(
        "com.distocraft.dc5000.etl.scheduler_testExecutionFailed/Expected.xml"));
    ITable expectedTable = expectedDataSet.getTable("META_SCHEDULINGS");

    Assertion.assertEquals(expectedTable, actualTable);
  }



  /**
   * Testing if scheduling description is formatted to correct format for
   * logging purposes.
   */
  @Test
  public void testGetDesc() throws Exception {

    // Creating Meta_schedulings object with certain values and asserting that
    // description is formatted correctly
    Meta_schedulings ms = new Meta_schedulings(null);
    ms.setExecution_type("fileExists");
    ms.setTrigger_command("SomeFile.txt");
    String expected = "Waiting file SomeFile.txt";
    String actual = (String) getDescription.invoke(objUnderTest, new Object[] { ms });
    assertEquals(expected, actual);

    ms.setExecution_type("weekly");
    ms.setWed_flag("Y");
    ms.setScheduling_hour(10L);
    ms.setScheduling_min(10L);
    expected = "Every Wed at 10:10";
    actual = (String) getDescription.invoke(objUnderTest, new Object[] { ms });

    // Testing if exception is thrown when inserting null Meta_schedulings
    // object
    ms = null;
    try {
      getDescription.invoke(objUnderTest, new Object[] { ms });
      fail("Exception expected - null Meta_schedulings object inserted as parameter");
    } catch (Exception e) {
      // Test passed - Exception catched
    }
  }

  /**
   * Testing if getTechpackName method returns correct tech Pack name in String
   * format. The name is also put into hashmap for later use.
   */
  @Test
  public void testGetTechpackName() throws Exception {

    // Initializing RockFactory and Meta_schedulings objects
    RockFactory rf = new RockFactory("jdbc:hsqldb:mem:testdb", "sa", "", "org.hsqldb.jdbcDriver", "con", true);
    Meta_schedulings ms = new Meta_schedulings(rf);
    initRock.invoke(objUnderTest, new Object[] {});

    // Testing if exception is thrown when trying to fetch data from empty table
    try {
      getTechpackName.invoke(objUnderTest, new Object[] { ms });
    } catch (Exception e) {
      // Test passed - Exception thrown
    }

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testTechpack', 'testing thread canceling', '1.0', 'Y', 'A')");

    // Testing if correct techpack name is returned
    String expected = "testTechpack";
    String actual = (String) getTechpackName.invoke(objUnderTest, new Object[] { ms });
    assertEquals(expected, actual);

    // Clearing Meta_collections to see if previously inserted techpack name can
    // be fetched via HashMap
    stmt.execute("DELETE FROM META_COLLECTION_SETS");
    expected = "testTechpack";
    actual = (String) getTechpackName.invoke(objUnderTest, new Object[] { ms });
    assertEquals(expected, actual);
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(SchedulerThreadTest.class);
  }

  /**
   * This class emulates running transferEngine and is implemented for testing
   * purposes only. Therefore it does nothing.
   */
  public class TransferEngineStub extends ITransferEngineRMIStub {

	  private static final long serialVersionUID = 1L;

    public TransferEngineStub() throws RemoteException {
      super();
    }
  }
}