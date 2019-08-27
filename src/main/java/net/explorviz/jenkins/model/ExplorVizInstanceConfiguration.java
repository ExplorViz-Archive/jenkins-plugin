package net.explorviz.jenkins.model;

import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

// TODO: Get rid of this class and make ExplorVizInstance hold the configuration and be the Describable?
public class ExplorVizInstanceConfiguration implements Describable<ExplorVizInstanceConfiguration> {
    private final ExplorVizDefinition explorVizDefinition;

    @DataBoundConstructor
    public ExplorVizInstanceConfiguration(ExplorVizDefinition explorVizDefinition) {
        this.explorVizDefinition = explorVizDefinition;
    }

    @Override
    public Descriptor<ExplorVizInstanceConfiguration> getDescriptor() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance == null) {
            throw new IllegalStateException();
        }
        return instance.getDescriptorOrDie(getClass());
    }

    public ExplorVizDefinition getExplorVizDefinition() {
        return explorVizDefinition;
    }

    @Extension
    @Symbol("explorVizInstanceConfiguration") // TODO: Symbol name is too long
    public static class DescriptorImpl extends Descriptor<ExplorVizInstanceConfiguration> {
        public ListBoxModel doFillExplorVizDefinitionItems() {
            ListBoxModel items = new ListBoxModel();
            // TODO: Load from global ExplorVizDefinitionConfiguration
            items.add(new ListBoxModel.Option("Built-in 1.4.0 (default)", "builtin1.4.0", true));
            return items;
        }

        public ListBoxModel doFillInstrumentationItems(@AncestorInPath Job job) {
            ListBoxModel items = new ListBoxModel();

            boolean first = true;
            for (InstrumentationAction action : job.getActions(InstrumentationAction.class)) {
                InstrumentationRecord record = action.getRecord();

                String displayName = record.getId();
                if (Util.fixEmptyAndTrim(record.getName()) != null) {
                    displayName = record.getName() + " (" + record.getId() + ")";
                }

                items.add(new ListBoxModel.Option(displayName, record.getId(), first));
                first = false;
            }

            return items;
        }

        public FormValidation doCheckExplorVizDefinition(@QueryParameter String value) {
            // TODO: Check if definition exists in ExplorVizDefinitionConfiguration or is built-in
            return FormValidation.validateRequired(value);
        }
    }
}
