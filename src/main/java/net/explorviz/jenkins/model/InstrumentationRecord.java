package net.explorviz.jenkins.model;

import hudson.Util;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Stores data logged during an instrumentation, including records that kieker logged.
 */
public class InstrumentationRecord implements Serializable {
    private static final long serialVersionUID = -3832303988591534239L;

    public static final int EXIT_CODE_SUCCESS = 0;
    public static final int EXIT_CODE_KILLED = 130;

    private final String id;
    private final String name;
    private final String kiekerLogFolderName;
    private final int applicationExitCode;

    /**
     * @param id                  An alphanumerical identifier for this instrumentation, unique within a single build
     * @param name                A display name for this instrumentation, to be read by humans. May be {@code null} to
     *                            only show "ExplorViz".
     * @param kiekerLogFolderName The folder name where kieker records have been saved to.
     * @param applicationExitCode The exit code of the application process
     */
    public InstrumentationRecord(String id, String name, @Nonnull String kiekerLogFolderName,
                                 int applicationExitCode) {
        this.id = id;
        this.name = Util.fixEmptyAndTrim(name);
        this.kiekerLogFolderName = kiekerLogFolderName;
        this.applicationExitCode = applicationExitCode;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // TODO: Replace with actual kieker record data
    public String getKiekerLogFolderName() {
        return kiekerLogFolderName;
    }

    /**
     * Returns the exit code of the application during instrumentation. {@code 130} means application was killed
     * (e.g. because timeout was hit) and is not considered an abnormal exit condition.
     *
     * @see #EXIT_CODE_SUCCESS
     * @see #EXIT_CODE_KILLED
     */
    public int getApplicationExitCode() {
        return applicationExitCode;
    }

    public boolean isAbnormalExit() {
        return applicationExitCode != EXIT_CODE_SUCCESS && applicationExitCode != EXIT_CODE_KILLED;
    }
}
