package net.explorviz.jenkins.model;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class ExplorVizDefinition implements Describable<ExplorVizDefinition> {
    private final String name;
    private final String composeDefinition;

    @DataBoundConstructor
    public ExplorVizDefinition(String name, String composeDefinition) {
        this.name = name;
        this.composeDefinition = composeDefinition;
    }

    @Override
    public Descriptor<ExplorVizDefinition> getDescriptor() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance == null) {
            throw new IllegalStateException();
        }
        return instance.getDescriptorOrDie(getClass());
    }

    public String getName() {
        return name;
    }

    public String getComposeDefinition() {
        return composeDefinition;
    }

    @Extension
    @Symbol("explorVizDefinition") // TODO: Symbol name is too long
    public static class DescriptorImpl extends Descriptor<ExplorVizDefinition> {
        // TODO: Form on ExplorVizAction databound to this class

        public FormValidation doCheckName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckComposeDefinition(@QueryParameter String value) {
            // TODO: Can we do any meaningful validation of docker compose files?
            return FormValidation.validateRequired(value);
        }
    }
}
