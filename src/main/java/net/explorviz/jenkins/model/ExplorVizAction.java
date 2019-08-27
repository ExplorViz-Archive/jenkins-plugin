package net.explorviz.jenkins.model;

import hudson.model.Run;
import jenkins.model.RunAction2;
import net.explorviz.jenkins.Messages;
import org.kohsuke.stapler.StaplerProxy;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Added to builds to display the {@code ExplorViz} entry in the build menu.
 */
public class ExplorVizAction implements RunAction2, StaplerProxy {
    /**
     * We may only keep a transient copy the parent objects, obtained in {@link #onLoad(Run)}
     */
    private transient Run<?, ?> run;

    @CheckForNull
    public Run getRun() {
        return this.run;
    }

    @Nonnull
    public InstrumentationRecord[] getRecords() {
        return run.getActions(InstrumentationAction.class).stream().map(InstrumentationAction::getRecord)
                .toArray(InstrumentationRecord[]::new);
    }

    /*
     * RunAction2
     */
    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
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
        // null hides the activity from the menu
        return this.run.hasPermission(ExplorVizGlobalConfiguration.VIEW) ?
                "/plugin/explorviz-plugin/images/24x24/explorviz.png" : null;
    }

    @Override
    public String getDisplayName() {
        return Messages.ExplorVizAction_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "explorviz";
    }

    /*
     * StaplerProxy
     */

    @Override
    public Object getTarget() {
        this.run.checkPermission(ExplorVizGlobalConfiguration.VIEW);
        return this; // TODO: Or ExplorVizInstanceConfiguration?
    }
}
