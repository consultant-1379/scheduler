package com.distocraft.dc5000.etl.scheduler;

import com.distocraft.dc5000.common.RmiUrlFactory;
import com.distocraft.dc5000.common.ServicenamesHelper;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.etl.rock.Meta_collection_sets;
import com.distocraft.dc5000.etl.rock.Meta_collection_setsFactory;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;
import com.distocraft.dc5000.etl.rock.Meta_schedulingsFactory;
import com.ericsson.eniq.scheduler.exception.SchedulerException;
import com.ericsson.eniq.repository.ETLCServerProperties;

/**
 * Program to control Scheduler
 */
public class SchedulerAdmin {

  private static final Logger log = Logger.getLogger("SchedulerAdmin");

  private String serverHostName;

  private int serverPort;

  private String serverRefName = "Scheduler";

  public SchedulerAdmin() throws SchedulerException {
    getProperties();
  }

  public SchedulerAdmin(final String serverHostName, final int serverPort) {
    this.serverHostName = serverHostName;
    this.serverPort = serverPort;
  }

   protected static void showUsage() {
    System.out.println("Usage: scheduler COMMAND");
    System.out.println("  commands:");
    System.out.println("    start");
    System.out.println("    stop");
    System.out.println("    status");
    System.out.println("    activate");
    System.out.println("    hold");
  }

  protected static void showScheduleStatusChangeUsage() {
    System.out.println("Usage: scheduler -e ACTION TECHPACK SCHEDULE_NAME");
    System.out.println("  ACTION:");
    System.out.println("    disable_schedule");
    System.out.println("    enable_schedule");
  }


  private static void exit(final exit_code code){
    System.exit(code.ordinal());
  }

  public static void main(final String args[]) {
    final exit_code result = admin(args);
    exit(result);
  }
  private static exit_code admin(final String[] args){
    try {
      System.setSecurityManager(new com.distocraft.dc5000.etl.engine.ETLCSecurityManager());
      if (args.length < 1) {
        showUsage();
        return exit_code.usage_2;
      }
      final SchedulerAdmin admin = new SchedulerAdmin();
      if (args[0].equalsIgnoreCase("stop")) {
        admin.shutdown();
        return exit_code.ok_0;
      } else if (args[0].equalsIgnoreCase("activate")) {
        admin.activate();
        return exit_code.ok_0;
      } else if (args[0].equalsIgnoreCase("hold")) {
        admin.hold();
        return exit_code.ok_0;
      } else if (args[0].equalsIgnoreCase("status")) {
        admin.status();
        return exit_code.ok_0;
      } else if (args[0].equalsIgnoreCase("reloadLoggingProperties")) {
        admin.reloadLoggingProperties();
        return exit_code.ok_0;
      } else if (args[0].equalsIgnoreCase("-e")) {
        // Extended command
        if (args.length < 2) {
          System.out.println("Unknown command \"" + args[0] + "\"");
          showUsage();
          return exit_code.usage_2;
        } else {

          if (args[1].equalsIgnoreCase("enable_schedule") || args[1].equalsIgnoreCase("disable_schedule")) {
            if (args.length < 4) {
              System.out.println("Not enough parameters for command \"" + args[1] + "\"");
              showScheduleStatusChangeUsage();
              return exit_code.usage_2;
            } else {
              return admin.changeScheduleStatus(args[1], args[2], args[3]);
            }
          } else {
            System.out.println("Unknown command \"" + args[1] + "\"");
            showUsage();
            return exit_code.usage_2;
          }
        }

      } else {
        System.out.println("Unknown command \"" + args[0] + "\"");
        showUsage();
        return exit_code.usage_2;
      }

    } catch (java.rmi.UnmarshalException e) {
      // Exception, cos connection breaks, when engine is shutdown
      return exit_code.conn_break_4;
    } catch (java.rmi.ConnectException e) {
      System.err.println("Connection to scheduler failed. (Connection)");
      return exit_code.conn_failed_connect_3;
    } catch (java.rmi.NotBoundException e) {
      System.err.println("Connection to scheduler failed. (Not bound)");
      return exit_code.conn_failed_notbound_5;
    } catch (Exception e) {
    	String mssg = "Caught exception in SchedulerAdmin:admin()... \n ";
    	mssg = mssg + e.toString();
    	log.log(Level.INFO, mssg);
      //System.err.println("\n");
      //e.printStackTrace(System.err);
      return exit_code.error_1;
    }
  }



  private void getProperties() throws SchedulerException {

    String sysPropDC5000 = System.getProperty("dc5000.config.directory");
    if (sysPropDC5000 == null) {
      sysPropDC5000 = "/dc/dc5000/conf/";
    }
    if (!sysPropDC5000.endsWith(File.separator)) {
      sysPropDC5000 += File.separator;
    }
    Properties appProps = null;
    try{
    	appProps = new ETLCServerProperties(sysPropDC5000 + "ETLCServer.properties");
    }catch(IOException e){
    	throw new SchedulerException(" Failed to read ETLCServer.properties ",e);
    }
    //Get the service name for scheduler (or at least host/ip from service_names)
    final String schedulerServiceName;
    try {
      schedulerServiceName = ServicenamesHelper.getServiceHost("scheduler", "localhost");
    } catch (IOException e) {
      throw new SchedulerException("Failed to get scheduler service_name entry", e);
    }
    //Does ETLCServer.properties override anything
    this.serverHostName = appProps.getProperty("SCHEDULER_HOSTNAME", schedulerServiceName);
    if (this.serverHostName == null) { // trying to determine hostname
      this.serverHostName = "localhost";

      try {
        this.serverHostName = InetAddress.getLocalHost().getHostName();
      } catch (java.net.UnknownHostException ex) {
        // default to localhost
      }

    }

    this.serverPort = 1200;
    final String sporttmp = appProps.getProperty("SCHEDULER_PORT", "1200");
    try {
      this.serverPort = Integer.parseInt(sporttmp);
    } catch (NumberFormatException nfe) {
      // default to 1200
    }

    this.serverRefName = appProps.getProperty("SCHEDULER_REFNAME", "Scheduler");
    

  }

  private void reloadLoggingProperties() throws SchedulerException {

    ISchedulerRMI scheduler = null;
    try{
    scheduler = connect();
    } catch(Exception e){
    	throw new SchedulerException(" Failed to connect ",e);
    }
    System.out.println("Reloading Scheduler logging properties...");

    log.log(Level.INFO, "Reloading Scheduler logging properties...");
    try{
    scheduler.reloadLoggingProperties();
    }catch(RemoteException e){
    	throw new SchedulerException(" Failed to connect ",e);
    }
    System.out.println("Reload logging properties successfully requested");
  }

  private ISchedulerRMI connect() throws Exception {
    //final String rmiURL = "//" + serverHostName + ":" + serverPort + "/" + serverRefName;

    return (ISchedulerRMI) Naming.lookup(RmiUrlFactory.getInstance().getSchedulerRmiUrl());

  }
  
  private void shutdown() throws SchedulerException {
	  ISchedulerRMI scheduler = null;
	  try{
     scheduler = connect();
	  }catch(Exception e){
		  throw new SchedulerException(" Failed to connect",e);
	  }
    System.out.println("Shutting down...");

    log.log(Level.INFO, "Shutting down Scheduler...");
    try{
    scheduler.shutdown();
    }catch(RemoteException e){
    	throw new SchedulerException(" Failed to shut down scheduler",e);
    }
    System.out.println("Shutdown successfully requested");

  }

  public void setRMI(final String serverHostName, final int serverPort) {
    this.serverHostName = serverHostName;
    this.serverPort = serverPort;
  }

  public void activate_silent() throws SchedulerException {
	 ISchedulerRMI scheduler = null;
	 try{
	 scheduler = connect();
    scheduler.reload();
	 }catch(RemoteException e){
		 throw new SchedulerException(" Failed to reload ",e);
	 }catch(Exception e){
		 throw new SchedulerException(" Failed to connect ",e);
	 }
  }

  public boolean testConnection() throws Exception {

    try {

       connect();

    } catch (Exception e) {

      return false;

    }

    return true;

  }

  private void activate() throws SchedulerException {
	 ISchedulerRMI scheduler = null;
    try{
	 scheduler = connect();
    }catch(Exception e){
    	throw new SchedulerException(" Failed to connect",e);
    }
    System.out.println("Activating Scheduler...");

    log.log(Level.INFO, "Starting Scheduler...");
    try{
    scheduler.reload();
    }catch(RemoteException e){
    	throw new SchedulerException(" Failed to reload scheduler",e);
    }
    System.out.println("Start successfully requested");

  }

  private void hold() throws Exception {

    final ISchedulerRMI scheduler = connect();

    System.out.println("Holding Scheduler...");

    log.log(Level.INFO, "Holding Scheduler...");

    scheduler.hold();

    System.out.println("Hold successfully requested");
  }

  private void status() throws SchedulerException {
	  try{
    final ISchedulerRMI scheduler = connect();
    System.out.println("Getting status...");
    final List<String> al = scheduler.status();
    for (String t : al) {
      System.out.println(t);
    }
    System.out.println("Finished successfully");
	  }catch(RemoteException e){
		  throw new SchedulerException(" Failed to connect to RMI",e);
	  }catch(Exception e){
		  throw new SchedulerException(" Failed to check scheduler status ",e);
	  }
  }

  public void trigger(final String name) throws SchedulerException {

    getProperties();
    try{
    final ISchedulerRMI scheduler = connect();

    log.log(Level.INFO, "Triggering a set (" + name + ") in scheduler...");

    scheduler.trigger(name);
    }catch(RemoteException e){
    	throw new SchedulerException(" Failed to trigger scheduler ",e);
    }catch(Exception e){
    	throw new SchedulerException(" Failed to trigger scheduler ",e);
    }
  }

  @Deprecated
  public void trigger(final List<String> list) throws Exception {

    getProperties();
    final ISchedulerRMI scheduler = connect();
    scheduler.trigger(list);

  }

  public void trigger(final List<String> list, final Map<String, String> map) throws SchedulerException {

    getProperties();
    try{
    final ISchedulerRMI scheduler = connect();
    scheduler.trigger(list, map);
    }catch(RemoteException e){
    	throw new SchedulerException(" Failed to connect ",e);
    }catch(Exception e){
    	throw new SchedulerException(" Failed to trigger scheduler ",e);
    }
  }

  public void trigger(final String name, final String context) throws SchedulerException {

    getProperties();
    try{
    final ISchedulerRMI scheduler = connect();

    log.log(Level.INFO, "Triggering a set (" + name + ") with context in scheduler...");

    scheduler.trigger(name, context);
    }catch(RemoteException e){
    	throw new SchedulerException(" Failed to trigger scheduler",e);
    }catch(Exception e){
    	throw new SchedulerException(" Failed to connect ",e);
    }
  }

  /**
   * This function disables/enables the specified schedule.
   * 
   * @param command
   *          Either "enable_schedule" or "disable_schedule".
   * @param techpack
   *          Name of the techpack that the schedule is related to.
   * @param scheduleList
   *          Comma seperated list of names of the schedule to disable or enable.
   * @return Returns true if the status change was successfull.
   */
  private exit_code changeScheduleStatus(final String command, final String techpack, final String scheduleList) {

    RockFactory etlrepRockFactory = null;

    try {
      final ETLCServerProperties props = new ETLCServerProperties();
      final Map<String, String> databaseConnectionDetails = props.getDatabaseConnectionDetails();
      etlrepRockFactory = createEtlrepRockFactory(databaseConnectionDetails);

      final Meta_collection_sets whereMetaCollSet = new Meta_collection_sets(etlrepRockFactory);
      whereMetaCollSet.setCollection_set_name(techpack);
      whereMetaCollSet.setEnabled_flag("Y");

      final Meta_collection_setsFactory metaCollSetFact = new Meta_collection_setsFactory(etlrepRockFactory, whereMetaCollSet);

      final Vector<?> metaCollSets = metaCollSetFact.get();

      if (metaCollSets.size() < 1) {
        System.out.println("Could not find active techpack named " + techpack + ". Exiting...");
        return exit_code.techpack_not_found_6;
      } else {
        final Meta_collection_sets targetMetaCollSet = (Meta_collection_sets) metaCollSets.get(0);

        final String[] scheduleNames = scheduleList.split(",");
        for(String schedule : scheduleNames){
          final Meta_schedulings whereMetaSchedulings = new Meta_schedulings(etlrepRockFactory);
          whereMetaSchedulings.setCollection_set_id(targetMetaCollSet.getCollection_set_id());
          whereMetaSchedulings.setName(schedule);

          final Meta_schedulingsFactory metaSchedulingsFact = new Meta_schedulingsFactory(etlrepRockFactory,
              whereMetaSchedulings);

          final Vector<Meta_schedulings> metaSchedulings = metaSchedulingsFact.get();

          if (metaSchedulings.size() < 1) {
            System.out.println("Could not find any schedule named " + schedule + " for techpack " + techpack
                + ". Exiting...");
            return exit_code.schedule_not_found_7;
          } else if (metaSchedulings.size() > 1) {
            System.out.println("Found more than one schedule named " + schedule + " for techpack " + techpack
                + ". Exiting...");
            return exit_code.multi_schedule_name_8;
          } else {

            final Meta_schedulings targetMetaScheduling = metaSchedulings.get(0);

            if (command.equalsIgnoreCase("enable_schedule")) {
              if (targetMetaScheduling.getHold_flag().equalsIgnoreCase("N")) {
                System.out.println("Schedule " + schedule + " is already enabled.");
              } else {
                targetMetaScheduling.setHold_flag("N");
                targetMetaScheduling.updateDB();
                System.out.println("Schedule " + schedule + " enabled successfully. Please reload scheduler for the changes to take effect.");
              }
            } else if (command.equalsIgnoreCase("disable_schedule")) {
              if (targetMetaScheduling.getHold_flag().equalsIgnoreCase("Y")) {
                System.out.println("Schedule " + schedule + " is already disabled.");
              } else {
                targetMetaScheduling.setHold_flag("Y");
                targetMetaScheduling.updateDB();
                System.out.println("Schedule " + schedule + " disabled successfully. Please reload scheduler for the changes to take effect.");
              }
            } else {
              System.out.println("Unknown command " + command + ". Exiting...");
              return exit_code.unknown_command_9;
            }
          }
        }
        return exit_code.ok_0;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Changing schedule status failed.");
      return exit_code.error_1;
    } finally {
      if (etlrepRockFactory != null) {
        try {
          etlrepRockFactory.getConnection().close();
        } catch (Exception e) {
          // meh
        }
      }
    }
  }

  /**
   * This function creates the rockfactory object to etlrep from the database
   * connection details read from ETLCServer.properties file.
   * 
   * @param databaseConnectionDetails conn details
   * @return Returns the created RockFactory.
   * @throws Exception On Errors
   */
  private RockFactory createEtlrepRockFactory(final Map<String, String> databaseConnectionDetails) throws SchedulerException {
    final String databaseUsername = databaseConnectionDetails.get("etlrepDatabaseUsername");
    final String databasePassword = databaseConnectionDetails.get("etlrepDatabasePassword");
    final String databaseUrl = databaseConnectionDetails.get("etlrepDatabaseUrl");
    final String databaseDriver = databaseConnectionDetails.get("etlrepDatabaseDriver");

    try {
      return new RockFactory(databaseUrl, databaseUsername, databasePassword, databaseDriver, "PreinstallCheck",
          true);

    } catch (Exception e) {
      e.printStackTrace();
      throw new SchedulerException("Unable to initialize database connection.", e);
    }
  }

  enum exit_code {
    ok_0, error_1, usage_2, conn_failed_connect_3, conn_break_4, conn_failed_notbound_5,
    techpack_not_found_6, schedule_not_found_7, multi_schedule_name_8, unknown_command_9
  }
}
