package net.explorviz.jenkins.model;

import hudson.model.InvisibleAction;

/**
 * Used to store {@link InstrumentationRecord}s with the build. Not directly visible from the build page.
 * <p>
 * These actions are loaded by {@link ExplorVizAction}.
 */
public class InstrumentationAction extends InvisibleAction {
    private final InstrumentationRecord record;

    public InstrumentationAction(InstrumentationRecord record) {
        this.record = record;
    }

    /**
     * @see InstrumentationRecord#getId()
     */
    public String getId() {
        return record.getId();
    }

    /**
     * @return The record stored with this action.
     */
    public InstrumentationRecord getRecord() {
        return record;
    }
}
