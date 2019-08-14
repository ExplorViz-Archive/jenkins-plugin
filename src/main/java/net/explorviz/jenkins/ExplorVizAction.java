package net.explorviz.jenkins;

import hudson.model.Action;

public class ExplorVizAction implements Action {
    private final String kiekerLogFolderName;

    public ExplorVizAction(String kiekerLogFolderName) {
        this.kiekerLogFolderName = kiekerLogFolderName;
    }

    public String getKiekerLogFolderName() {
        return kiekerLogFolderName;
    }

    @Override
    public String getIconFileName() {
        return "explorviz.png";
    }

    @Override
    public String getDisplayName() {
        return "ExplorViz";
    }

    @Override
    public String getUrlName() {
        return "explorviz";
    }
}
