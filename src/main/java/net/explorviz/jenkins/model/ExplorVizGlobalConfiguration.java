package net.explorviz.jenkins.model;

import hudson.Extension;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.explorviz.jenkins.Messages;
import org.jenkinsci.Symbol;

/**
 * TODO: ExplorVizGlobalConfiguration to modify available ExplorVizDefinitions and choose default one
 */
@Extension
@Symbol("explorviz")
public class ExplorVizGlobalConfiguration extends GlobalConfiguration {
    // TODO: Maybe make own GlobalConfigurationCategory with own MODIFY permission?

    /*
     * Not exactly clear what owner of PermissionGroup is supposed to be; persisted permission data probably depends
     * on existence of this class, i.e. jenkins won't load/display the permissions if this class goes missing.
     */
    public static final PermissionGroup PERMISSIONS = new PermissionGroup(ExplorVizGlobalConfiguration.class,
            Messages._ExplorVizGlobalConfiguration_Permissions_Title());
//    /**
//     * This permissions allows modifying the global ExplorViz configuration, i.e. modifying ExplorVizDefinitions.
//     */
//    public static final Permission MODIFY = new Permission(PERMISSIONS, "Configure",
//            Messages._ExplorVizGlobalConfiguration_MODIFY_description(), Jenkins.ADMINISTER, PermissionScope.JENKINS);
    /**
     * Allows visualizing a build ({@link hudson.model.Run}) in ExplorViz. Administrators may want to disable this for
     * unauthenticated users because of the resource-intensiveness and therefore DoS potential.
     */
    public static final Permission RUN = new Permission(PERMISSIONS, "Run",
            Messages._ExplorVizGlobalConfiguration_RUN_description(), Jenkins.ADMINISTER, PermissionScope.RUN);
    /**
     * Allows viewing the ExplorViz page and the list of recorded instrumentations.
     */
    public static final Permission VIEW = new Permission(PERMISSIONS, "View",
            Messages._ExplorVizGlobalConfiguration_VIEW_description(), RUN, PermissionScope.RUN);
}
