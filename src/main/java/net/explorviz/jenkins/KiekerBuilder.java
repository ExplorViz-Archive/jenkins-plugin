package net.explorviz.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.QuotedStringTokenizer;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class KiekerBuilder extends Builder implements SimpleBuildStep {
    private static final String ARG_JAVA_AGENT = "-javaagent:";
    private static final String ARG_KIEKER_MONITORING_CONFIGURATION = "-Dkieker.monitoring.configuration=";
    private static final String ARG_ASPECTJ_WEAVER_CONFIGURATION = "-Dorg.aspectj.weaver.loadtime.configuration=";

    private final String appJar;
    private String appArgs;
    private final String aopXml;
    private String vmOpts;
    private String kiekerJar;
    private boolean failBuildOnEmpty;

    @DataBoundConstructor
    public KiekerBuilder(String aopXml, String appJar) {
        this.appJar = appJar;
        this.appArgs = "";
        this.aopXml = aopXml;
        this.vmOpts = "";
        this.kiekerJar = "";
        this.failBuildOnEmpty = true;
    }

    public String getAopXml() {
        return aopXml;
    }

    public String getAppJar() {
        return appJar;
    }

    public String getAppArgs() {
        return appArgs;
    }

    @DataBoundSetter
    public void setAppArgs(String appArgs) {
        this.appArgs = appArgs;
    }

    public String getVmOpts() {
        return vmOpts;
    }

    @DataBoundSetter
    public void setVmOpts(String vmOpts) {
        this.vmOpts = vmOpts;
    }

    public String getKiekerJar() {
        return kiekerJar;
    }

    @DataBoundSetter
    public void setKiekerJar(String kiekerJar) {
        this.kiekerJar = kiekerJar;
    }

    public boolean isFailBuildOnEmpty() {
        return failBuildOnEmpty;
    }

    @DataBoundSetter
    public void setFailBuildOnEmpty(boolean failBuildOnEmpty) {
        this.failBuildOnEmpty = failBuildOnEmpty;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        // TODO: Correct kieker path
        String javaagent = kiekerJar.isEmpty() ? "kieker-1.14-SNAPSHOT-aspectj.jar" : kiekerJar;

        // TODO: Automatically generate kieker monitoring configuration
        String kiekerMonitoringConfiguration = "META-INF/kieker.monitoring.configuration";

        // TODO: Find proper java executable based on Jenkins tool config
        ArgumentListBuilder argList = new ArgumentListBuilder();
        argList.add("java");
        argList.add(ARG_JAVA_AGENT + javaagent);
        argList.add(ARG_ASPECTJ_WEAVER_CONFIGURATION + aopXml);
        argList.add(ARG_KIEKER_MONITORING_CONFIGURATION + kiekerMonitoringConfiguration);
        argList.add(QuotedStringTokenizer.tokenize(vmOpts));
        argList.add("-jar", appJar);
        if (!appArgs.isEmpty()) {
            argList.add("--");
            argList.add(QuotedStringTokenizer.tokenize(appArgs));
        }

        launcher.launch().stdout(listener).pwd(workspace).cmds(argList).join();
    }

    @Symbol("kieker")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "ExplorViz: Run Kieker instrumentation";
        }

        public FormValidation doCheckAppJar(@QueryParameter String value) {
            // TODO: Warn if file not found
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckAppArgs(@QueryParameter String value) {
            return FormValidation.ok();
        }

        public FormValidation doCheckAopXml(@QueryParameter String value) {
            // TODO: Warn if file not found
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckVmOpts(@QueryParameter String value) {
            Set<FormValidation> warnings = new HashSet<>(0);

            if (value.contains("-javaagent")) {
                warnings.add(FormValidation.warning("Do not specify -javaagent! It is automatically set to kieker."));
            }
            if (value.contains("-jar")) {
                warnings.add(FormValidation.warning("Do not specify -jar! It is automatically set."));
            }
            if (value.contains("-Dkieker.monitoring.configuration")) {
                warnings.add(FormValidation.warning("Do not specify the kieker monitoring configuration!" +
                    "It is auto-generated and other configurations won't work."));
            }
            if (value.contains("-Dorg.aspectj.weaver.loadtime.configuration")) {
                warnings.add(FormValidation.warning("Do not specify the aspectj weaver configuration!" +
                    "It is auto-generated and other configurations won't work."));
            }

            return FormValidation.aggregate(warnings);
        }

        public FormValidation doCheckKiekerJar(@QueryParameter String value) {
            // TODO: Warn if file not found
            return FormValidation.ok();
        }
    }
}
