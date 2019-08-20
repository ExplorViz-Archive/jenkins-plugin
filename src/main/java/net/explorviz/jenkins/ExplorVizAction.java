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
        return "/plugin/explorviz-plugin/images/24x24/explorviz.png";
    }

    @Override
    public String getDisplayName() {
        return "Visualize in ExplorViz";
    }

    @Override
    public String getUrlName() {
        return "explorviz";
    }
}
