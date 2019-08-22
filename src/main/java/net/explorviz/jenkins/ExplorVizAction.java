package net.explorviz.jenkins;

import hudson.Util;
import hudson.model.Action;

import javax.annotation.Nonnull;

public class ExplorVizAction implements Action {
    private final String runId;
    private final String runName;
    private final String kiekerLogFolderName;

    /**
     * @param runId               An alphanumerical identifier for this instrumentation, unique within a single build
     * @param runName             A display name for this instrumentation, to be read by humans. May be {@code null} to
     *                            only show "ExplorViz".
     * @param kiekerLogFolderName The folder name where kieker records have been saved to
     */
    public ExplorVizAction(String runId, String runName, @Nonnull String kiekerLogFolderName) {
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

    @Override
    public String getIconFileName() {
        return "/plugin/explorviz-plugin/images/24x24/explorviz.png";
    }

    @Override
    public String getDisplayName() {
        return runName == null ? "ExplorViz" : ("ExplorViz: " + runName);
    }

    @Override
    public String getUrlName() {
        return "explorviz/" + runId;
    }
}
