package net.explorviz.jenkins;

import net.explorviz.jenkins.model.ExplorVizInstanceConfiguration;
import net.explorviz.jenkins.model.InstrumentationRecord;

import java.net.URL;

/**
 * Represents one ExplorViz instance managed through docker (compose).
 * <p>
 * TODO: ExplorVizInstance
 */
public class ExplorVizInstance {
    private final InstrumentationRecord record;
    private final ExplorVizInstanceConfiguration settings;

    // TODO: Check for ExplorVizGlobalConfiguration.RUN permission for launch/kill

    public ExplorVizInstance(InstrumentationRecord record, ExplorVizInstanceConfiguration settings) {
        this.record = record;
        this.settings = settings;
    }

    /**
     * Launch this ExplorViz instance.
     */
    public void launch() {
        // TODO: Remove, nonse code to make SpotBugs shut the fuck up
        throw new UnsupportedOperationException(
                record.getId() + settings.getExplorVizDefinition().getComposeDefinition());
    }

    /**
     * Kill this ExplorViz instance, shutting it down and deleting its images (if any).
     */
    public void kill() {

    }

    /**
     * @return The URL on which to access this ExplorViz instance.
     */
    public URL getExplorVizURL() {
        return null;
    }
}
