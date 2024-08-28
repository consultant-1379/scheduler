package com.distocraft.dc5000.etl.scheduler;

import static org.junit.Assert.*;
import com.ericsson.eniq.common.testutilities.ServicenamesTestHelper;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import junit.framework.JUnit4TestAdapter;
import org.dbunit.Assertion;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ssc.rockfactory.RockFactory;

/**
 * Tests for SchedulerAdmin class in com.distocraft.dc5000.etl.scheduler.<br>
 * <br>
 * Testing various commands used to control and create schedule processes.
 * 
 * @author EJAAVAH
 * 
 */
public class SchedulerAdminTest {

  private static SchedulerAdmin objUnderTest;

  private static File ETLCServerProperties;

  private static Connection con = null;

  private static Statement stmt;

  protected Boolean holdTestFlag = false;

  protected Boolean reloadTestFlag = false;

  protected Boolean reloadLoggingPropertiesTestFlag = false;

  protected Boolean shutdownTestFlag = false;

  protected Boolean statusTestFlag = false;

  protected Boolean triggerByNameTestFlag = false;

  protected Boolean triggerByListTestFlag = false;

  protected Boolean triggerByMapTestFlag = false;

  protected Boolean triggerByNameAndContextTestFlag = false;
  
  private static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"), "SchedulerAdminTest");
  private final ISchedulerRMIStub schedulerStub;

  public SchedulerAdminTest() throws Exception {

    // Initializing RMI object to be used by ThreadScheduler
    schedulerStub = new ISchedulerRMIStub(this);
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    
    
    if(!TMP_DIR.exists() && !TMP_DIR.mkdirs()){
      fail("Failed to create temp directory...");
    }
    
    ServicenamesTestHelper.setupEmpty(TMP_DIR);
    ServicenamesTestHelper.createDefaultServicenamesFile();
    System.setProperty("ETC_DIR", TMP_DIR.getPath());

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

    // Setting system property for config filepath
    System.setProperty("dc5000.config.directory", TMP_DIR.getPath());
    System.setProperty("dc.config.dir", TMP_DIR.getPath());

    // Creating ETLCServer property file
    ETLCServerProperties = new File(TMP_DIR.getPath(), "ETLCServer.properties");
    try {
      PrintWriter pw = new PrintWriter(new FileWriter(ETLCServerProperties));
      pw.write("SCHEDULER_HOSTNAME = localhost\n");
      pw.write("SCHEDULER_PORT = 1100\n");
      pw.write("SCHEDULER_REFNAME = TestScheduler\n");
      pw.write("ENGINE_DB_URL = jdbc:hsqldb:mem:testdb\n");
      pw.write("ENGINE_DB_USERNAME = sa\n");
      pw.write("ENGINE_DB_PASSWORD= \n");
      pw.write("ENGINE_DB_DRIVERNAME = org.hsqldb.jdbcDriver\n");
      pw.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    
    ETLCServerProperties.delete();
    stmt.execute("DROP TABLE META_SCHEDULINGS");
    stmt.execute("DROP TABLE META_COLLECTION_SETS");
    objUnderTest = null;
  }

  @Before
  public void setupBeforeTest() throws Exception {
    // Initializing often used objects/methods before every test to avoid mix
    // ups
    objUnderTest = new SchedulerAdmin();
  }

  @After
  public void clearUpAfterTest() throws Exception {
    // Clearing objects and tables used in testing
    stmt.execute("DELETE FROM META_SCHEDULINGS");
    stmt.execute("DELETE FROM META_COLLECTION_SETS");
    reloadTestFlag = false;
    objUnderTest = null;
  }

  /**
   * Testing properties loading from property file.
   */
  @Test
  public void testGetProperties() throws Exception {

    // Initializing SchedulerAdmin instance, reflecting the tested method and
    // values the properties are given to
    Class saClass = objUnderTest.getClass();
    Method getProperties = saClass.getDeclaredMethod("getProperties", new Class[] {});
    getProperties.setAccessible(true);
    Field serverHostName = saClass.getDeclaredField("serverHostName");
    serverHostName.setAccessible(true);
    Field serverPort = saClass.getDeclaredField("serverPort");
    serverPort.setAccessible(true);
    Field serverRefName = saClass.getDeclaredField("serverRefName");
    serverRefName.setAccessible(true);

    // Initializing reflected methods as null in order to see if getProperties
    // method works
    serverHostName.set(objUnderTest, null);
    serverPort.setInt(objUnderTest, 0);
    serverRefName.set(objUnderTest, null);

    // Invoking getProperties() method and asserting that properties are set
    // correctly
    getProperties.invoke(objUnderTest, new Object[] {});
    String actual = serverHostName.get(objUnderTest) + ", " + serverPort.getInt(objUnderTest) + ", "
        + serverRefName.get(objUnderTest);
    String expected = "localhost, 1100, TestScheduler";
  }

  /**
   * Testing reloading logging properties.
   */
  @Test
  public void testReloadLoggingProperties() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method reloadLoggingProperties = saClass.getDeclaredMethod("reloadLoggingProperties", new Class[] {});
    reloadLoggingProperties.setAccessible(true);

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    reloadLoggingProperties.invoke(objUnderTest, new Object[] {});
    assertEquals(true, reloadLoggingPropertiesTestFlag);
  }

  /**
   * Testing connecting to scheduler object via RMI.
   */
  @Test
  public void testConnect() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method connect = saClass.getDeclaredMethod("connect", new Class[] {});
    connect.setAccessible(true);

    // Invoking connect() method and saving returned scheduler object for
    // assertion
    ISchedulerRMI scheduler = (ISchedulerRMI) connect.invoke(objUnderTest, new Object[] {});
    assertNotNull(scheduler);
  }

  @Test
  public void testRestartScheduler() throws Exception {
    // TODO: Testing scheduler restarting - How Runtime.getRuntime().exec()
    // works?
  }

  /**
   * Testing shutting down scheduler.
   */
  @Test
  public void testShutdown() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method shutdown = saClass.getDeclaredMethod("shutdown", new Class[] {});
    shutdown.setAccessible(true);

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    shutdown.invoke(objUnderTest, new Object[] {});
    assertEquals(true, shutdownTestFlag);
  }

  /**
   * Testing RMI variable setting.
   */
  @Test
  public void testSetRMI() throws Exception {

    // Reflecting tested variables
    Class saClass = objUnderTest.getClass();
    Field serverHostName = saClass.getDeclaredField("serverHostName");
    serverHostName.setAccessible(true);
    Field serverPort = saClass.getDeclaredField("serverPort");
    serverPort.setAccessible(true);

    // Calling setRMI() method and checking if parameters are delivered
    objUnderTest.setRMI("testhost", 888);
    assertEquals("testhost", serverHostName.get(objUnderTest));
    assertEquals(888, serverPort.getInt(objUnderTest));
  }

  /**
   * Testing silent activation of scheduler thread (no logging or console
   * outputs).
   */
  @Test
  public void testActivate_silent() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method activate_silent = saClass.getDeclaredMethod("activate_silent", new Class[] {});
    activate_silent.setAccessible(true);

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    activate_silent.invoke(objUnderTest, new Object[] {});
    assertEquals(true, reloadTestFlag);
  }

  /**
   * Testing if connection is up and running or not.
   */
  @Test
  public void testTestConnection() throws Exception {

    // Reflecting serverHostName and setting it null
    Class saClass = objUnderTest.getClass();
    Field serverHostName = saClass.getDeclaredField("serverHostName");
    serverHostName.setAccessible(true);
    serverHostName.set(objUnderTest, null);


    schedulerStub.stop();

    // Asserting that false is returned when RMI object is called because server
    // host name is null and cannot be found
    assertEquals(false, objUnderTest.testConnection());

    schedulerStub.start();

    // Initializing SchedulerAdmin with correct connection properties and
    // asserting that connection is found (true returned from testConnection())
    objUnderTest = new SchedulerAdmin();
    assertEquals(true, objUnderTest.testConnection());
  }

  /**
   * Testing scheduler thread activating.
   */
  @Test
  public void testActivate() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method activate = saClass.getDeclaredMethod("activate", new Class[] {});
    activate.setAccessible(true);

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    activate.invoke(objUnderTest, new Object[] {});
    assertEquals(true, reloadTestFlag);
  }

  /**
   * Test putting scheduler thread on hold.
   */
  @Test
  public void testHold() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method hold = saClass.getDeclaredMethod("hold", new Class[] {});
    hold.setAccessible(true);

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    hold.invoke(objUnderTest, new Object[] {});
    assertEquals(true, holdTestFlag);
  }

  /**
   * Test getting scheduler status.
   */
  @Test
  public void testStatus() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method status = saClass.getDeclaredMethod("status", new Class[] {});
    status.setAccessible(true);

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    status.invoke(objUnderTest, new Object[] {});
    assertEquals(true, statusTestFlag);
  }

  /**
   * Test schedule triggering by its name.
   */
  @Test
  public void testTriggerByName() throws Exception {

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    objUnderTest.trigger("Test");
    assertEquals(true, triggerByNameTestFlag);
  }

  /**
   * Test schedule triggering by list.
   */
  @Test
  public void testTriggerByList() throws Exception {

    // Initializing test List
    List testList = new Vector();
    testList.add("test");

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    objUnderTest.trigger(testList);
    assertEquals(true, triggerByListTestFlag);
  }

  /**
   * Test schedule triggering by map.
   */
  @Test
  public void testTriggerByMap() throws Exception {

    // Initializing test list
    List<String> testList = new ArrayList();
    testList.add("test");
    
    // Initializing test Map
    Map<String, String> testMap = new HashMap<String, String>();
    testMap.put("test", "test");

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    objUnderTest.trigger(testList, testMap);
    assertEquals(true, triggerByMapTestFlag);
  }

  /**
   * Test schedule triggering by name and context.
   */
  @Test
  public void testTriggerByNameAndContext() throws Exception {

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
    objUnderTest.trigger("testName", "testContext");
    assertEquals(true, triggerByNameAndContextTestFlag);
  }
  
   
  /**
   * Testing status changing method. Schedule status can be changed by giving
   * techpack and schedule name and command value as parameters.
   */
  @Test
  public void testChangeScheduleStatus() throws Exception {

    // Inserting test data into tables
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'techpack', 'testing scheduler thread creating', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'Once', 'oscommand', 1, 1, 1, 1, 0, 1, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule1', 'N', 1, 2008, 'triggercommand',0)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 1, 'FileExists', 'oscommand', 1, 1, 1, 1, 0, 1, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule2', 'Y', 1, 2008, 'triggercommand',0)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 2, 'Weekly', 'oscommand', 1, 1, 1, 1, 0, 1, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule3', 'N', 1, 2008, 'triggercommand',0)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 3, 'Interval', 'oscommand', 1, 1, 1, 1, 0, 1, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule3', 'N', 1, 2008, 'triggercommand',0)");

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method changeScheduleStatus = saClass.getDeclaredMethod("changeScheduleStatus", new Class[] { String.class,
        String.class, String.class });
    changeScheduleStatus.setAccessible(true);

    // Asserting that false is returned when no such techpack is found as given
    // in parameters
    /*
     * assertEquals(false, changeScheduleStatus.invoke(objUnderTest, new
     * Object[] { "command", "notExistingTechpack", "schedule" }));
     */

    // Asserting that false is returned when no schedules or more than one
    // schedule with same name is found
    /*
     * assertEquals(false, changeScheduleStatus.invoke(objUnderTest, new
     * Object[] { "command", "techpack", "testschedule3" }));
     */

    // Asserting that false is returned when status is tried to change with
    // invalid command
    /*
     * assertEquals(false, changeScheduleStatus.invoke(objUnderTest, new
     * Object[] { "randomCommand", "techpack", "testschedule1" }));
     */

    // Asserting that changes are made into the database when status changes are
    // made (true is also returned)
    assertEquals(SchedulerAdmin.exit_code.ok_0, changeScheduleStatus.invoke(objUnderTest, new Object[] { "disable_schedule", "techpack",
        "testschedule1" }));
    assertEquals(SchedulerAdmin.exit_code.ok_0, changeScheduleStatus.invoke(objUnderTest, new Object[] { "enable_schedule", "techpack",
        "testschedule2" }));

    IDataSet actualDataSet = new DatabaseConnection(con).createDataSet();
    ITable actualTable = actualDataSet.getTable("Meta_Schedulings");


    final String lookFor = "XMLFiles";
    final URL url = ClassLoader.getSystemResource(lookFor);
    if (url == null) {
      throw new Exception("Couldn't find '" + lookFor + "' on classpath");
    }
    final String baseDir = url.toURI().getRawPath();

    IDataSet expectedDataSet = new FlatXmlDataSet(new File(baseDir,
        "/com.distocraft.dc5000.etl.schedulerAdmin_testChangeScheduleStatus/Expected.xml"));
    ITable expectedTable = expectedDataSet.getTable("Meta_Schedulings");

    Assertion.assertEquals(expectedTable, actualTable);
  }

  /**
   * Testing creation of Etlrep RockFactory object using HashMap object given as
   * parameter.
   */
  @Test
  public void testCreateEtlrepRockFactory() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method createEtlrepRockFactory = saClass
        .getDeclaredMethod("createEtlrepRockFactory", new Class[] { Map.class });
    createEtlrepRockFactory.setAccessible(true);

    // Initializing HashMap with connection details
    HashMap condetails = new HashMap();
    condetails.put("etlrepDatabaseUsername", "sa");
    condetails.put("etlrepDatabasePassword", "");
    condetails.put("etlrepDatabaseUrl", "jdbc:hsqldb:mem:testdb");
    condetails.put("etlrepDatabaseDriver", "org.hsqldb.jdbcDriver");

    // Invoking tested method and checking RockFactory object with correct
    // values is returned
    RockFactory rockFact = (RockFactory) createEtlrepRockFactory.invoke(objUnderTest, new Object[] { condetails });
    String actual = rockFact.getUserName() + ", " + rockFact.getPassword() + ", " + rockFact.getDbURL() + ", "
        + rockFact.getDriverName();
    String expected = "sa, , jdbc:hsqldb:mem:testdb, org.hsqldb.jdbcDriver";
  }

  /**
   * Testing Exception handling of createEtlrepRockFactory() method using
   * HashMap initialized with empty values.
   */
  @Test
  public void testCreateEtlrepRockFactoryWithEmptyMap() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method createEtlrepRockFactory = saClass
        .getDeclaredMethod("createEtlrepRockFactory", new Class[] { Map.class });
    createEtlrepRockFactory.setAccessible(true);

    // Initializing HashMap with connection details
    HashMap condetails = new HashMap();
    condetails.put("etlrepDatabaseUsername", "");
    condetails.put("etlrepDatabasePassword", "");
    condetails.put("etlrepDatabaseUrl", "");
    condetails.put("etlrepDatabaseDriver", "");

    // Invoking tested method and checking if exception is thrown because of
    // empty map given as parameter
    try {
      createEtlrepRockFactory.invoke(objUnderTest, new Object[] { condetails });
      fail("Test Failed - Exception expected as HashMap with empty values was given as parameter");
    } catch (Exception e) {
      // Test passed - Exception catched
    }
  }
/*
  @Test
  public void testGetDatabaseConnectionDetails() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method getDatabaseConnectionDetails = saClass.getDeclaredMethod("getDatabaseConnectionDetails", new Class[] {});
    getDatabaseConnectionDetails.setAccessible(true);

    // Invoking getDatabaseConnectionDetails() method and asserting that
    // returned HashMap has same values as in property file the properties are
    // read from
    HashMap condetails = (HashMap) getDatabaseConnectionDetails.invoke(objUnderTest, new Object[] {});
    String actual = condetails.get("etlrepDatabaseUrl") + ", " + condetails.get("etlrepDatabaseUsername") + ", "
        + condetails.get("etlrepDatabasePassword") + ", " + condetails.get("etlrepDatabaseDriver");
    String expected = "jdbc:hsqldb:mem:testdb, sa, , org.hsqldb.jdbcDriver";
    assertEquals(expected, actual);
  }

  @Test
  public void testGetDatabaseConnectionDetailsWithInvalidFilepath() throws Exception {

    // Reflecting the tested method
    Class saClass = objUnderTest.getClass();
    Method getDatabaseConnectionDetails = saClass.getDeclaredMethod("getDatabaseConnectionDetails", new Class[] {});
    getDatabaseConnectionDetails.setAccessible(true);

    // Setting invalid filepath
    System.setProperty("CONF_DIR", "NotExistingPath");

    // Invoking getDatabaseConnectionDetails() method and checking that
    // exception is thrown
    try {
      getDatabaseConnectionDetails.invoke(objUnderTest, new Object[] {});
      fail("Test failed - Exception expected because of invalid filepath");
    } catch (Exception e) {
      // Test Passed - exception thrown
    }
  }*/

  @Test
  public void callShowUsageAndScheduleStatusChangeUsage() throws Exception {

    // Invoking the tested method and asserting that proper flag in set to true
    // via ISchedulerRMIStub
	  String arg0[] = {"start"};  
	  SchedulerAdmin.showUsage();
	  SchedulerAdmin.showScheduleStatusChangeUsage();
	 
	  
  }


 
  
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(SchedulerAdminTest.class);
  }

}
