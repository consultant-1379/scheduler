package com.distocraft.dc5000.etl.scheduler.trigger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.sql.Timestamp;
import java.util.Date;

import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.distocraft.dc5000.etl.rock.Meta_schedulings;

/**
 * Tests for Interval class in com.distocraft.dc5000.etl.scheduler.trigger.<br>
 * <br>
 * Testing set triggering with interval.
 *
 * @author EJAAVAH
 *
 */
public class IntervalTest {

    private static Interval objUnderTest;

    private static Meta_schedulings schedulings;

    private static Timestamp tstmp;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        // Meta_schedulings for initializing Interval time
        schedulings = new Meta_schedulings(null);
        schedulings.setInterval_hour(0L);
        schedulings.setInterval_min(15L);

        try {
            objUnderTest = new Interval();
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
     * Testing if set is triggered on first time it is run and when the triggering
     * interval (1h) is exceeded. When trigger is released execute method in
     * Interval class returns true, otherwise false.
     */
    @Test
    public void testIntervalExecute() throws Exception {
        // Testing if the trigger is released if the last execution time is more than
        // an hour in the future
        tstmp = new Timestamp(new Date().getTime() + 3600050);
        schedulings.setLast_execution_time(tstmp);
        assertEquals(true, objUnderTest.execute());

        // Testing if the trigger is released if the last execution time is more than
        // 15 mins (as set up) in the past
        tstmp = new Timestamp(new Date().getTime() - 900050);
        schedulings.setLast_execution_time(tstmp);
        assertEquals(true, objUnderTest.execute());

        // Trigger should not be released when last execute time is less than an
        // 15 mins (as set up) in the past
        tstmp = new Timestamp(new Date().getTime() - 890050);
        schedulings.setLast_execution_time(tstmp);
        assertEquals(false, objUnderTest.execute());

        // Testing if interval is triggered on the first execution (last execution =
        // null)
        tstmp = null;
        schedulings.setLast_execution_time(tstmp);
        assertEquals(true, objUnderTest.execute());
    }

    @Test
    public void testDstStart() throws Exception {
        // Sets should trigger on correct time when current time comes under DST
        final long diffOffSet = 1;
        final long lastExeOffset = 0;
        final long currTimeOffSet = 3600000;
        final Date curDate = new Date();
        final long time = curDate.getTime();
        boolean isDst;
        //	  long time = 1344952088250l;   //14-08-2012;

        final long tmpTime = objUnderTest.calculateOffset(diffOffSet, time, currTimeOffSet, lastExeOffset);
        if (tmpTime < time) {
            isDst = true;
        } else {
            isDst = false;
        }
        assertEquals(true, isDst);

    }

    @Test
    public void testDstEnd() throws Exception {
        // Sets should trigger on correct time when current time comes under DST
        final long diffOffSet = -1;
        final long currTimeOffSet = 0;
        final long lastExeOffset = 3600000;
        final  Date curDate = new Date();
        final long time = curDate.getTime();
        boolean isDst;

        //	  long time = 1326548998421l; //14-01-2012;
        final long tmpTime = objUnderTest.calculateOffset(diffOffSet, time, currTimeOffSet, lastExeOffset);
        if (tmpTime > time) {
            isDst = true;
        } else {
            isDst = false;
        }
        assertEquals(true, isDst);

    }

    /**
     * Testing a case when Interval hour and minute in schedulings are initialized
     * with 0 value. This will cause ArithmeticException as division by zero is
     * tried.
     */
    @Test
    public void testIntervalException() throws Exception {
        // Initializing schedulings interval hour and minute with 0 value
        schedulings.setInterval_hour(0L);
        schedulings.setInterval_min(0L);
        tstmp = new Timestamp(new Date().getTime());
        schedulings.setLast_execution_time(tstmp);
        try {
            objUnderTest.execute();
            fail("Exception expected - Division by zero in Interval class");
        } catch (ArithmeticException ae) {
            // Test passed - ArithmeticException expected
        } catch (Exception e) {
            fail("Unexpected error occured - ArithmeticException expected\n" + e);
        }
    }

    // Making the test work with ant 1.6.5 and JUnit 4.x
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(IntervalTest.class);
    }
}
