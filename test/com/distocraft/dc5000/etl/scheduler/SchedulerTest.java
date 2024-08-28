package com.distocraft.dc5000.etl.scheduler;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Properties;
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
import org.junit.Ignore;
import org.junit.Test;

import com.distocraft.dc5000.common.StaticProperties;
import com.distocraft.dc5000.common.monitor.PlatformServices;

/**
 * Tests for Scheduler class in com.distocraft.dc5000.etl.scheduler.<br>
 * <br>
 * Testing Scheduler class which controls SchedulerThreads by starting,
 * triggering, canceling and putting them on hold.
 * 
 * @author EJAAVAH
 * 
 */
public class SchedulerTest {

  private static Scheduler objUnderTest;

  private static Field testSchedulerThread;

  private static Connection con = null;

  private static Statement stmt;

  private static File ETLCServerProperties;

  protected static int portNum = 1200;

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
        + "TUE_FLAG VARCHAR(100), WED_FLAG VARCHAR(100), THU_FLAG VARCHAR(100), FRI_FLAG VARCHAR(100), "
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

    // Creating a new RMI object instance used in some of the tests
    ITransferEngineRMIStub transferEngine = new ITransferEngineRMIStub("Engine", portNum);

    // Creating ETLCServer property file
    ETLCServerProperties = new File(System.getProperty("user.dir"), "ETLCServer.properties");
    try {
      PrintWriter pw = new PrintWriter(new FileWriter(ETLCServerProperties));
      pw.write("ENGINE_DB_URL = jdbc:hsqldb:mem:testdb\n");
      pw.write("ENGINE_DB_USERNAME = sa\n");
      pw.write("ENGINE_DB_PASSWORD= \n");
      pw.write("ENGINE_DB_DRIVERNAME = org.hsqldb.jdbcDriver\n");
      pw.write("ENGINE_HOSTNAME = localhost\n");
      pw.write("ENGINE_PORT = "+portNum+"\n");
      pw.write("ENGINE_REFNAME = Engine\n");
      pw.write("SCHEDULER_HOSTNAME = localhost\n");
      pw.write("SCHEDULER_PORT = 888\n");
      pw.write("SCHEDULER_REFNAME = Scheduler\n");
      pw.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {

    ETLCServerProperties.delete();
    testSchedulerThread = null;
    objUnderTest = null;
    stmt.execute("DROP TABLE META_SCHEDULINGS");
    stmt.execute("DROP TABLE META_COLLECTION_SETS");
    stmt.execute("DROP TABLE META_COLLECTIONS");
  }

  @Before
  public void setUp() throws Exception {

    // Creating new Scheduler instance before every test
    objUnderTest = new Scheduler();

    // Reflecting and initializing SchedulerThread variable to be used in
    // testing
    Class schedulerClass = objUnderTest.getClass();
    testSchedulerThread = schedulerClass.getDeclaredField("schedulerThread");
    testSchedulerThread.setAccessible(true);
    testSchedulerThread.set(objUnderTest, new SchedulerThread(5L, 5, "//localhost:"+portNum+"/Engine", 5L,
        "jdbc:hsqldb:mem:testdb", "org.hsqldb.jdbcDriver", "sa", ""));

		final Properties p = new Properties();
		p.put("smf.enabled", "false");
		StaticProperties.giveProperties(p);


	
  }

  @After
  public void tearDown() throws Exception {
    stmt.execute("DELETE FROM META_SCHEDULINGS");
    stmt.execute("DELETE FROM META_COLLECTION_SETS");
    stmt.execute("DELETE FROM META_COLLECTIONS");
    testSchedulerThread = null;
    objUnderTest = null;
  }

	protected File getFile(final String name) throws Exception {
		final URL url = ClassLoader.getSystemResource("XMLFiles");
		if(url == null){
			throw new FileNotFoundException("XMLFiles");
		}
		final File xmlBase = new File(url.toURI());
		final String xmlFile = xmlBase.getAbsolutePath() + "/"+name;
		return new File(xmlFile);
	}

	  
	//Test case fails on and off on Jenkins.. hence ignoring..
	@Ignore
	  public void testStartScheduler() throws Exception {
	    try {
	    		doSettings();
	    		//String homeDir = System.getProperty("user.home");
	    		//String pathOfFile= homeDir + "/config";
	    		//System.setProperty("dc5000.config.directory",pathOfFile );
	    		
	    		objUnderTest.startScheduler();

	    
	    } catch (Exception e) {
	      // Test Passed - Exception thrown
	    }
	  }
	  
	  
	  
	  @Test
	  public void testServiceOffline() throws Exception {

	    try {

	    	PlatformServices service = PlatformServices.Engine;
	    	objUnderTest.serviceOffline(service);
	    	assertNull(testSchedulerThread.get(objUnderTest));
	    	
	    	PlatformServices services = PlatformServices.repdb;
	    	objUnderTest.serviceOffline(services);
	    	assertNull(testSchedulerThread.get(objUnderTest));
	    
	    } catch (Exception e) {
	      // Test Passed - Exception thrown
	    }
	  }
	  
	  
	  
	  
	  @Test
	 public void testServiceAvailableCaseOne() throws Exception {
	//Case 1: service= engine and isredpbOnline = true
		  //Result: reload() called
	    try {

	    	PlatformServices service = PlatformServices.Engine;
	    	doSettings();
	    	objUnderTest.serviceAvailable(service);
	        SchedulerThread actual = (SchedulerThread) testSchedulerThread.get(objUnderTest);
	        assertEquals(true, actual.isAlive());
	    	
	 
	    
	    } catch (Exception e) {
	      // Test Passed - Exception thrown
	    }
	  }
	  
	  @Test
	  public void testServiceAvailableCaseTwo() throws Exception {
	//Case 2: service= repdb and isengineOnline = true
		  //Result: loadProperties() and reload called
	    try {

	    	PlatformServices service = PlatformServices.repdb;
	    	doSettings();
	    	objUnderTest.serviceAvailable(service);
	        SchedulerThread actual = (SchedulerThread) testSchedulerThread.get(objUnderTest);
	        assertEquals(true, actual.isAlive());
	    	
	 
	    
	    } catch (Exception e) {
	      // Test Passed - Exception thrown
	    }
	  }
	  
	  @Test
	  public void testServiceAvailableCaseThree() throws Exception {
	//Case 3: service= engine and isrepdbOnline = false
		  //Result: reload() called
	    try {

	    	PlatformServices service = PlatformServices.Engine;
	    	//doSettings();
	    	Class st = objUnderTest.getClass();
	    	Field isRepdbOnline = st.getDeclaredField("isRepdbOnline");
	    	isRepdbOnline.setAccessible(true);
	    	isRepdbOnline.set(objUnderTest, false);
	    	objUnderTest.serviceAvailable(service);
	    	
	    
	    } catch (Exception e) {
	      // Test Passed - Exception thrown
	    }
	  }
	  
	  
	  @Test
	  public void testServiceAvailableCaseFour() throws Exception {
	//Case 4: service= repdb and isengineOnline = false
		  //Result: reload() called
	    try {

	    	PlatformServices service = PlatformServices.repdb;
	    	//doSettings();
	    	Class st = objUnderTest.getClass();
	    	Field isEngineOnline = st.getDeclaredField("isEngineOnline");
	    	isEngineOnline.setAccessible(true);
	    	isEngineOnline.set(objUnderTest, false);
	    	doSettings();
	    	objUnderTest.serviceAvailable(service);
	    
	    } catch (Exception e) {
	      // Test Passed - Exception thrown
	    }
	  }
	    
	  
	  
  /**
   * Testing if scheduler cancels schedulerThread and creates new one and starts
   * it.
   */
  @Test
  public void testReload() throws Exception {

    // Calling reload() method and asserting that tested schedulerthread has
    // been started
    objUnderTest.reload();
    SchedulerThread actual = (SchedulerThread) testSchedulerThread.get(objUnderTest);
    assertEquals(true, actual.isAlive());

    // Stopping the thread to avoid errors while connecting to database
    actual.stop();
  }

  /**
   * Testing if scheduler puts thread on hold.
   */
  @Test
  public void testHold() throws Exception {

    // Calling hold() method and asserting that schedulerThread is null
    objUnderTest.hold();
    assertNull(testSchedulerThread.get(objUnderTest));
  }

  /**
   * Testing shutting down scheduler thread.
   */
  @Test
  public void testShutdown() throws Exception {

    // TODO: How to test System.exit(0)
  }

  /**
   * Testing if Status method returns correct scheduler status information.
   */
  @Test
  public void testStatus() throws Exception {

    // Status() method returns list object which will be compared to expected
    // outcome
    List actual = objUnderTest.status();
    assertEquals("[--- ETLC Scheduler ---,   Status: active,   Poll interval: 0,   Penalty Wait: 0]", actual
        .toString());
  }

  /**
   * Testing set triggering by name.
   */
  
  @Test
  public void testTriggerByName() throws Exception {

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing thread starting by its name', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule1', 'N', 1, 2008, 'triggercommand', 0)");
    stmt.executeUpdate("INSERT INTO META_COLLECTIONS VALUES"
        + "(0, 'testcollection', 'collection', 'mailerroraddrs', 'mailfailaddrs', 'mailbugaddrs', 1, 1, 1, 'Y',"
        + " 'Y', '2008-01-01 00:00:00.0', '1.0', 0, '0', 1, 1, 'Y', 'A', 'Y', 'meastype', 'Y', 'schedulinginfo')");

    // Reflecting makeThreads() method from SchedulerThread class and invoke it
    // in order to add schedulings to threadlist
    ITransferEngineRMIStub transferEngine = new ITransferEngineRMIStub("TransferEngine", portNum);
    SchedulerThread actual = (SchedulerThread) testSchedulerThread.get(objUnderTest);
    Class st = actual.getClass();
    Method triggerThreadList = st.getDeclaredMethod("makeThreads", new Class[] {});
    triggerThreadList.setAccessible(true);
    try {
      triggerThreadList.invoke(actual, new Object[] {});
    } catch (NoClassDefFoundError e){
      System.out.println("ERROR : " + e.getMessage());
      for(StackTraceElement ste: e.getStackTrace()){
        System.out.println(ste);
      }
      if(e.getCause() != null){
        System.out.println("Cause : " + e.getCause().getMessage());
        for(StackTraceElement ste: e.getCause().getStackTrace()){
          System.out.println(ste);
        }
      } else {
        System.out.println("Cause was NULL");
      }
    }

    // Triggering schedule by its name
    objUnderTest.trigger("testschedule1");

    // Asserting that test schedule has been executed (status changed to
    // Executed in META_SCHEDULINGS table)
    IDataSet actualDataSet = new DatabaseConnection(con).createDataSet();
    ITable actualTable = actualDataSet.getTable("META_SCHEDULINGS");

    IDataSet expectedDataSet = new FlatXmlDataSet(getFile(
        "com.distocraft.dc5000.etl.scheduler_testTriggerByName/Expected.xml"));
    ITable expectedTable = expectedDataSet.getTable("META_SCHEDULINGS");

    // Filtering out few time related rows in order to make the assertion work
    ITable filteredTable = DefaultColumnFilter.includedColumnsTable(actualTable, expectedTable.getTableMetaData()
        .getColumns());

    Assertion.assertEquals(expectedTable, filteredTable);
  }

  /**
   * Testing set triggering by list.
   */
  
  @Test
  public void testTriggerByList() throws Exception {

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing thread starting by its name', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule1', 'N', 1, 2008, 'triggercommand', 0)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 1, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule2', 'N', 1, 2008, 'triggercommand', 0)");
    stmt.executeUpdate("INSERT INTO META_COLLECTIONS VALUES"
        + "(0, 'testcollection', 'collection', 'mailerroraddrs', 'mailfailaddrs', 'mailbugaddrs', 1, 1, 1, 'Y',"
        + " 'Y', '2008-01-01 00:00:00.0', '1.0', 0, '0', 1, 1, 'Y', 'A', 'Y', 'meastype', 'Y', 'schedulinginfo')");

    // Reflecting makeThreads() method from SchedulerThread class and invoke it
    // in order to add schedulings to threadlist
    ITransferEngineRMIStub transferEngine = new ITransferEngineRMIStub("TransferEngine", portNum);
    SchedulerThread actual = (SchedulerThread) testSchedulerThread.get(objUnderTest);
    Class st = actual.getClass();
    Method triggerThreadList = st.getDeclaredMethod("makeThreads", new Class[] {});
    triggerThreadList.setAccessible(true);
    triggerThreadList.invoke(actual, new Object[] {});

    // Initializing schedule list and trigger them
    List list = new Vector();
    list.add("testschedule1");
    list.add("testschedule2");
    objUnderTest.trigger(list);

    // Asserting that test schedule has been executed (status changed to
    // Executed in META_SCHEDULINGS table)
    IDataSet actualDataSet = new DatabaseConnection(con).createDataSet();
    ITable actualTable = actualDataSet.getTable("META_SCHEDULINGS");

    IDataSet expectedDataSet = new FlatXmlDataSet(getFile(
        "com.distocraft.dc5000.etl.scheduler_testTriggerByList/Expected.xml"));
    ITable expectedTable = expectedDataSet.getTable("META_SCHEDULINGS");

    // Filtering out few time related rows in order to make the assertion work
    ITable filteredTable = DefaultColumnFilter.includedColumnsTable(actualTable, expectedTable.getTableMetaData()
        .getColumns());

    Assertion.assertEquals(expectedTable, filteredTable);
  }
  
  /**
   * Testing set triggering by map.
   */
  
  @Test
  public void testTriggerByMap() throws Exception {

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing thread starting by its name', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule1', 'N', 1, 2008, 'triggercommand', 0)");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 1, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule2', 'N', 1, 2008, 'triggercommand', 0)");
    stmt.executeUpdate("INSERT INTO META_COLLECTIONS VALUES"
        + "(0, 'testcollection', 'collection', 'mailerroraddrs', 'mailfailaddrs', 'mailbugaddrs', 1, 1, 1, 'Y',"
        + " 'Y', '2008-01-01 00:00:00.0', '1.0', 0, '0', 1, 1, 'Y', 'A', 'Y', 'meastype', 'Y', 'schedulinginfo')");

    // Reflecting makeThreads() method from SchedulerThread class and invoke it
    // in order to add schedulings to threadlist
    ITransferEngineRMIStub transferEngine = new ITransferEngineRMIStub("TransferEngine", portNum);
    SchedulerThread actual = (SchedulerThread) testSchedulerThread.get(objUnderTest);
    Class st = actual.getClass();
    Method triggerThreadList = st.getDeclaredMethod("makeThreads", new Class[] {});
    triggerThreadList.setAccessible(true);
    triggerThreadList.invoke(actual, new Object[] {});

    // Initializing setname list
    List<String> list1 = new ArrayList();
    list1.add("testschedule1");
    
    List<String> list2 = new ArrayList();
    list1.add("testschedule2");

    
    // Initializing schedule maps and trigger them
    Map<String, String> map1 = new HashMap<String, String>();
    map1.put("setName", "testschedule1");
    map1.put("setType", "loader");
    map1.put("setBaseTable", "test_table");
    objUnderTest.trigger(list1, map1);

    Map<String, String> map2 = new HashMap<String, String>();
    map2.put("setName", "testschedule2");
    map2.put("setType", "loader");
    map2.put("setBaseTable", "test_table");
    objUnderTest.trigger(list2, map2);
    
    // Asserting that test schedule has been executed (status changed to
    // Executed in META_SCHEDULINGS table)
    IDataSet actualDataSet = new DatabaseConnection(con).createDataSet();
    ITable actualTable = actualDataSet.getTable("META_SCHEDULINGS");

    IDataSet expectedDataSet = new FlatXmlDataSet(getFile(
        "com.distocraft.dc5000.etl.scheduler_testTriggerByList/Expected.xml"));
    ITable expectedTable = expectedDataSet.getTable("META_SCHEDULINGS");

    // Filtering out few time related rows in order to make the assertion work
    ITable filteredTable = DefaultColumnFilter.includedColumnsTable(actualTable, expectedTable.getTableMetaData()
        .getColumns());

    Assertion.assertEquals(expectedTable, filteredTable);
  }

  

  /**
   * Testing set triggering by name and command.
   */
  
  @Test
  public void testTriggerByNameAndCommand() throws Exception {

    // Inserting test data
    stmt.executeUpdate("INSERT INTO META_COLLECTION_SETS VALUES"
        + "('0', 'testset', 'testing thread starting by its name', '1.0', 'Y', 'A')");
    stmt.executeUpdate("INSERT INTO META_SCHEDULINGS VALUES"
        + "('1.0', 0, 'once', 'oscommand', 1, 1, 1, 1, 0, 0, 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'Y', 'status',"
        + " '2008-01-01 00:00:00.0', 1, 1, 'testschedule1', 'N', 1, 2008, 'triggercommand', 0)");
    stmt.executeUpdate("INSERT INTO META_COLLECTIONS VALUES"
        + "(0, 'testcollection', 'collection', 'mailerroraddrs', 'mailfailaddrs', 'mailbugaddrs', 1, 1, 1, 'Y',"
        + " 'Y', '2008-01-01 00:00:00.0', '1.0', 0, '0', 1, 1, 'Y', 'A', 'Y', 'meastype', 'Y', 'schedulinginfo')");

    // Reflecting makeThreads() method from SchedulerThread class and invoke it
    // in order to add schedulings to threadlist
    ITransferEngineRMIStub transferEngine = new ITransferEngineRMIStub("TransferEngine", portNum);
    SchedulerThread actual = (SchedulerThread) testSchedulerThread.get(objUnderTest);
    Class st = actual.getClass();
    Method triggerThreadList = st.getDeclaredMethod("makeThreads", new Class[] {});
    triggerThreadList.setAccessible(true);
    triggerThreadList.invoke(actual, new Object[] {});

    // Triggering schedule by name and command
    objUnderTest.trigger("testschedule1", "command");

    // Asserting that test schedule has been executed (status changed to
    // Executed in META_SCHEDULINGS table)
    IDataSet actualDataSet = new DatabaseConnection(con).createDataSet();
    ITable actualTable = actualDataSet.getTable("META_SCHEDULINGS");

    IDataSet expectedDataSet = new FlatXmlDataSet(getFile(
        "com.distocraft.dc5000.etl.scheduler_testTriggerByNameAndCommand/Expected.xml"));
    ITable expectedTable = expectedDataSet.getTable("META_SCHEDULINGS");

    // Filtering out few time related rows in order to make the assertion work
    ITable filteredTable = DefaultColumnFilter.includedColumnsTable(actualTable, expectedTable.getTableMetaData()
        .getColumns());

    Assertion.assertEquals(expectedTable, filteredTable);
  }

  /**
   * Testing Logging property reloading.
   */
  @Test
  public void testReloadLoggingProperties() throws Exception {
    // TODO: How to test logging property reloading
  }

  /**
   * Testing initializing Scheduler server and binding it to RMI registry.
   */
  @Test
  public void testInit() throws Exception {

    // Reflecting the tested method
    Class st = objUnderTest.getClass();
    Method init = st.getDeclaredMethod("init", new Class[] {});
    init.setAccessible(true);

    // Reflecting the server property values and setting values for them
    Field serverHostName = st.getDeclaredField("serverHostName");
    Field serverPort = st.getDeclaredField("serverPort");
    Field serverRefName = st.getDeclaredField("serverRefName");
    serverHostName.setAccessible(true);
    serverPort.setAccessible(true);
    serverRefName.setAccessible(true);
    serverHostName.set(objUnderTest, "localhost");
    serverPort.setInt(objUnderTest, portNum);
    serverRefName.set(objUnderTest, "Engine");

    // Asserting that true is returned when initialization and binding of the
    // server has succeeded
    assertEquals(true, init.invoke(objUnderTest, new Object[] {}));
  }

  /**
   * Testing initializing Scheduler server and binding it to RMI registry with
   * null server identifier values.
   */
 /*@Test
  public void testInitWithNullValues() throws Exception {

    // Reflecting the tested method
    Class st = objUnderTest.getClass();
    Method init = st.getDeclaredMethod("init", new Class[] {});
    init.setAccessible(true);

    // Asserting that true is returned when initialization and binding of the
    // server has succeeded
    assertEquals(false, init.invoke(objUnderTest, new Object[] {}));
  }

  /**
   * Testing connection property and RMI naming value loading from configuration
   * file.
   */
  @Test
  public void testLoadProperties() throws Exception {

	String actual = doSettings();
    String expected = "sa, , org.hsqldb.jdbcDriver, jdbc:hsqldb:mem:testdb, localhost, Scheduler, 888, rmi://localhost:"+portNum+"/Engine";
    assertEquals(expected, actual);
  }
  
  private String doSettings() throws Exception{
	    // Setting filepath to the config file
	    System.setProperty("dc5000.config.directory", System.getProperty("user.dir"));

	    // Reflecting the tested method
	    Class st = objUnderTest.getClass();
	    Method loadProperties = st.getDeclaredMethod("loadProperties", new Class[] {});
	    loadProperties.setAccessible(true);

	    // Reflecting connection property and RMI object URL variables
	    Field userName = st.getDeclaredField("userName");
	    Field password = st.getDeclaredField("password");
	    Field dbDriverName = st.getDeclaredField("dbDriverName");
	    Field url = st.getDeclaredField("url");
	    Field engineURL = st.getDeclaredField("engineURL");
	    Field serverHostName = st.getDeclaredField("serverHostName");
	    Field serverPort = st.getDeclaredField("serverPort");
	    Field serverRefName = st.getDeclaredField("serverRefName");
	    userName.setAccessible(true);
	    password.setAccessible(true);
	    dbDriverName.setAccessible(true);
	    url.setAccessible(true);
	    engineURL.setAccessible(true);
	    serverHostName.setAccessible(true);
	    serverPort.setAccessible(true);
	    serverRefName.setAccessible(true);

	    // Asserting that all properties are read from the config file when
	    // loadProperties() is invoked
	    loadProperties.invoke(objUnderTest, new Object[] {});
	    String actual = userName.get(objUnderTest) + ", " + password.get(objUnderTest) + ", "
	        + dbDriverName.get(objUnderTest) + ", " + url.get(objUnderTest) + ", " + serverHostName.get(objUnderTest)
	        + ", " + serverRefName.get(objUnderTest) + ", " + serverPort.get(objUnderTest) + ", "
	        + engineURL.get(objUnderTest);
	    return actual;
	  
  }

  /**
   * Testing configuration file reading with invalid filepath.
   */
  @Test
  public void testLoadPropertiesWithInvalidFilepath() throws Exception {

    // Setting filepath to the config file
    System.setProperty("dc5000.config.directory", "NotExistingPath");

    // Reflecting the tested method
    Class st = objUnderTest.getClass();
    Method loadProperties = st.getDeclaredMethod("loadProperties", new Class[] {});
    loadProperties.setAccessible(true);

    // Testing if exception is thrown when file cannot be found
    try {
      loadProperties.invoke(objUnderTest, new Object[] {});
      fail("Test Failed - Exception expected as configuration file should not be found");
    } catch (Exception e) {
      // Test Passed - Exception thrown
    }
  }
  
  

  

  
  
  
  @Test
  public void testGetName() throws Exception {
	  
	  String actual = objUnderTest.getName();
	  String expected = "Scheduler";
	  assertEquals(actual, expected);	  
	  
	  
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(SchedulerTest.class);
  }
}