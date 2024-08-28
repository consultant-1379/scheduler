package com.distocraft.dc5000.etl.scheduler.trigger;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import com.distocraft.dc5000.etl.scheduler.TimeTrigger;
import com.ericsson.eniq.scheduler.exception.SchedulerException;

/**
 * @author eromsza
 */
public class IntervalDirCheck extends TimeTrigger {

    protected String triggerCommand;

    protected long nextTriggerTime = 0L;

    @Override
    public void update() throws SchedulerException {

        if (this.schedule.getTrigger_command() != null) {
            this.triggerCommand = this.schedule.getTrigger_command();
        }

        super.update();
    }

    @Override
    public boolean execute() throws SchedulerException {

        update();

        boolean retValue = checkTime();

        if (retValue && triggerCommand != null) {
            final String[] commands = triggerCommand.split(";");
            if (commands.length != 2) {
                throw new SchedulerException("The trigger is not correctly initialized.");
            } else if (commands[0].isEmpty() || commands[1].isEmpty()) {
                throw new SchedulerException("The trigger is not correctly initialized.");
            } else {
                // if checkDirEmpty = true then check if the provided directories below are all empty,
                // if checkDirEmpty = false then check if any of them contains files
                final boolean checkDirEmpty = Boolean.valueOf(commands[0].trim());

                final String path = commands[1].trim();
                // directories are comma-separated
                final String[] dirNames = path.split(",");
                final int dirNameCount = dirNames.length;
                // create a list of parsed path names
                final List<File> nameList = new ArrayList<File>();
                for (String dirName : dirNames) {
                    nameList.add(new File(dirName.trim()));
                }
                // validate provided comma-separated directory paths
                if (dirNameCount > 0) {
                    // create a list of parsed directories
                    for (File dir : nameList) {
                        // create a list of directories applying '*' wildcard
                        final List<File> dirList = findSubdirectories(dir);
                        if (dirList.isEmpty()) {
                            throw new SchedulerException("The path: " + dir.getPath() + " is invalid.");
                        }
                        // check if the condition is met
                        for (File directory : dirList) {
                            // checking ALL directories are empty
                            if (checkDirEmpty) {
                                if (directory.list().length == 0) {
                                    retValue = true;
                                } else {
                                    return false;
                                }
                                // checking ANY directory is not empty
                            } else {
                                if (directory.list().length == 0) {
                                    retValue = false;
                                } else {
                                    return true;
                                }
                            }
                        }
                    }

                    return retValue;
                }
            }
        }

        return false;
    }

    /**
     * Recursively find all sub-directories of the directory with a path provided.
     *
     * @param root the root path
     * @return the List of the directories to check
     * @throws SchedulerException the SchedulerException
     */
    protected List<File> findSubdirectories(final File root) throws SchedulerException {
        // create a list of parsed directories
        final List<File> subdirs = new ArrayList<File>();

        // check the '*' wildcard
        if (root.getPath().contains("*")) {
            // create a list of subdirectories
            final File dir = new File(root.getPath().substring(0, root.getPath().indexOf("*")));
            final File[] dirList = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(final File pathname) {
                    return pathname.isDirectory();
                }
            });
            // no subdir means incorrect path
            if (dirList == null) {
                throw new SchedulerException("The path: " + root.getPath() + " is not a directory.");
            // if the list length is 0 then it is an absolute path without wildcards
            } else if (dirList.length == 0) {
                if (dir.isDirectory()) {
                    subdirs.add(dir);
                }
                return subdirs;
            }

            // Recursion to look for another nested wildcard
            for (File subdir : dirList) {
                subdirs.addAll(findSubdirectories(new File(subdir.getPath()
                        + root.getPath().substring(root.getPath().indexOf("*") + 1))));
            }
            return subdirs;
        }

        // no wildcard detected, return an original path
        if (root.isDirectory()) {
            subdirs.add(root);
        }
        // return the directory paths without '*' wildcard
        return subdirs;
    }

    /**
     * Check the suitable time interval for to trigger the set.
     *
     * @return true if the criteria for triggering are met, false otherwise.
     */    

    protected boolean checkTime() {
        final long currentTime = System.currentTimeMillis();

        //  interval Calculate milliseconds
        final long interval = (intervalHour * 60 * 60 * 1000) + (intervalMinute * 60 * 1000);

        if (currentTime + (60 * 60 * 1000) < lastExecutionTime && nextTriggerTime <= currentTime) {
            nextTriggerTime = currentTime / interval
                    * interval + interval;
            lastExecutionTime = currentTime;

            return true;
        } else if ((lastExecutionTime + interval) <= currentTime && nextTriggerTime <= currentTime) {

            final long timeDifference = (currentTime - lastExecutionTime) % interval;
            // calculate next interval
            nextTriggerTime = currentTime / interval
                    * interval + interval;
            lastExecutionTime = currentTime - timeDifference;

            return true;
        }

        return false;
    }
}
