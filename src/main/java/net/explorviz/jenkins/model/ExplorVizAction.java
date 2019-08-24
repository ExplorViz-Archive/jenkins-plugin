package net.explorviz.jenkins.model;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;

import java.util.Collection;
import java.util.Collections;

/**
 * Added to builds to display the {@code ExplorViz} entry in the build menu.
 */
public class ExplorVizAction implements RunAction2, SimpleBuildStep.LastBuildAction {
    /**
     * We may only keep a transient copy the parent objects, obtained in {@link #onLoad(Run)}
     */
    private transient Run<?,?> run;

    public String getTest() {
        return run.getActions(KiekerRecordAction.class).get(0).getKiekerLogFolderName();
    }

    /*
     * RunAction2
     */

    @Override
    public void onAttached(Run<?, ?> run) {
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    /*
     * Action
     */

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

    /*
     * LastBuildAction
     */

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.emptyList();
    }
}
