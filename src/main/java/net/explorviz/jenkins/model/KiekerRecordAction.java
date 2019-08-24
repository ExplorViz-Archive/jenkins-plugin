package net.explorviz.jenkins.model;

import hudson.Util;
import hudson.model.InvisibleAction;

import javax.annotation.Nonnull;

/**
 * Used to store the logged kieker records with the build. Not directly visible from the build.
 * <p>
 * These actions are instead loaded by {@link ExplorVizAction}.
 */
public class KiekerRecordAction extends InvisibleAction {
    private final String runId;
    private final String runName;
    private final String kiekerLogFolderName;

    /**
     * @param runId               An alphanumerical identifier for this instrumentation, unique within a single build
     * @param runName             A display name for this instrumentation, to be read by humans. May be {@code null} to
     *                            only show "ExplorViz".
     * @param kiekerLogFolderName The folder name where kieker records have been saved to
     */
    public KiekerRecordAction(String runId, String runName, @Nonnull String kiekerLogFolderName) {
        this.runId = runId;
        this.runName = Util.fixEmptyAndTrim(runName);
        this.kiekerLogFolderName = kiekerLogFolderName;
    }

    public String getRunId() {
        return runId;
    }

    public String getRunName() {
        return runName;
    }

    public String getKiekerLogFolderName() {
        return kiekerLogFolderName;
    }
}
