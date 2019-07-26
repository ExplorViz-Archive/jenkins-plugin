package net.explorviz.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;

@SuppressWarnings("unused")
public class ExplorVizBuilder extends Builder implements SimpleBuildStep {
    private final String version;

    @DataBoundConstructor
    public ExplorVizBuilder(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws IOException, InterruptedException {
        launcher.launch().stdout(listener).pwd(workspace)
            .cmds("/bin/sh", "-c", "echo Running ExplorViz " + getVersion()).join();
    }

    @Symbol("explorviz")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "ExplorViz: Run ExplorViz instrumentation";
        }

        public FormValidation doCheckVersion(@QueryParameter String value) {
            if (value.trim().length() <= 0) {
                return FormValidation.error("Version may not be empty.");
            }
            // TODO: Check if version tag is valid on relevant services
            return FormValidation.ok();
        }
    }
}
