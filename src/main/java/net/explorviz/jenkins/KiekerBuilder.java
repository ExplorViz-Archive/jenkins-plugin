package net.explorviz.jenkins;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.QuotedStringTokenizer;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import kieker.analysis.source.file.MapFileFilter;
import kieker.common.util.filesystem.FSUtil;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class KiekerBuilder extends Builder implements SimpleBuildStep {
    private static final String ARG_JAVA_AGENT = "-javaagent:";
    private static final String ARG_KIEKER_MONITORING_CONFIGURATION = "-Dkieker.monitoring.configuration=";
    private static final String ARG_ASPECTJ_WEAVER_CONFIGURATION = "-Dorg.aspectj.weaver.loadtime.configuration=";
    private static final String ARG_SKIP_DEFAULT_AOP_CONFIGURATION = "-Dkieker.monitoring.skipDefaultAOPConfiguration=true";

    private final String appJar;
    private String appArgs;
    private boolean skipDefaultAOP;
    private final String aopXml;
    private String vmOpts;
    private String kiekerJar;
    private boolean failBuildOnEmpty;

    @DataBoundConstructor
    public KiekerBuilder(String aopXml, String appJar) {
        // Must match defaults in KiekerBuilder/config.jelly
        this.appJar = appJar;
        this.appArgs = "";
        this.skipDefaultAOP = true;
        this.aopXml = aopXml;
        this.vmOpts = "";
        this.kiekerJar = "";
        this.failBuildOnEmpty = true;
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

    public boolean isSkipDefaultAOP() {
        return skipDefaultAOP;
    }

    @DataBoundSetter
    public void setSkipDefaultAOP(boolean skipDefaultAOP) {
        this.skipDefaultAOP = skipDefaultAOP;
    }

    public String getAopXml() {
        return aopXml;
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
        FilePath workingDirectory = workspace.createTempDir("kieker", null);

        String javaagent = Util.fixEmptyAndTrim(kiekerJar);
        if (javaagent == null) {
            // TODO: Copy/Deploy/Download bundled kieker jar to workingDirectory
            javaagent = "kieker-1.14-SNAPSHOT-aspectj.jar";
        }

        // Create kieker monitoring configuration
        KiekerMonitoringConfiguration monitoringConfiguration = new KiekerMonitoringConfiguration();
        monitoringConfiguration.setApplicationName(run.getParent().getFullDisplayName() + "_" + run.getId());
        monitoringConfiguration.setOutputDirectory(workingDirectory.getRemote());

        FilePath monitoringConfigurationFile = workingDirectory.child("kieker.monitoring.configuration");
        monitoringConfiguration.write(monitoringConfigurationFile);

        ArgumentListBuilder argList = new ArgumentListBuilder();
        // TODO: Make JDK selectable
        argList.add(Jenkins.get().getJDKs().get(0).getBinDir() + "/java");
        argList.add(ARG_JAVA_AGENT + javaagent);
        argList.add(ARG_ASPECTJ_WEAVER_CONFIGURATION + aopXml);
        argList.add(ARG_KIEKER_MONITORING_CONFIGURATION + monitoringConfigurationFile.getRemote());
        if (skipDefaultAOP) {
            argList.add(ARG_SKIP_DEFAULT_AOP_CONFIGURATION);
        }
        argList.add(QuotedStringTokenizer.tokenize(vmOpts));
        argList.add("-jar", appJar);
        if (!appArgs.trim().isEmpty()) {
            argList.add("--");
            argList.add(QuotedStringTokenizer.tokenize(appArgs));
        }

        Proc kiekerProc = launcher.launch().stdout(listener).pwd(workspace).cmds(argList).start();
        // TODO: Configurable timeout, don't fail build when hitting timeout
        if (kiekerProc.joinWithTimeout(60, TimeUnit.SECONDS, listener) != 0) {
            run.setResult(Result.FAILURE);
        }

        Optional<FilePath> recordDir = workingDirectory.listDirectories().stream().filter(KiekerBuilder::isKiekerDirectory).findFirst();
        if (recordDir.isPresent()) {
            listener.getLogger().println("Kieker records were saved to: " + recordDir.get().getRemote());
        } else {
            if (failBuildOnEmpty) {
                listener.error("No kieker records have been written. Failing build as a result.");
                run.setResult(Result.FAILURE);
            } else {
                listener.getLogger().println("No kieker records have been written.");
            }
        }
    }

    private static boolean isKiekerDirectory(FilePath path) {
        try {
            return !path.list(pathname -> pathname.getName().endsWith(FSUtil.MAP_FILE_EXTENSION)).isEmpty();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("MethodMayBeStatic")
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
            Collection<FormValidation> warnings = new HashSet<>(0);

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
                    "It is set automatically."));
            }

            return FormValidation.aggregate(warnings);
        }

        public FormValidation doCheckKiekerJar(@QueryParameter String value) {
            // TODO: Warn if file not found
            return FormValidation.ok();
        }
    }
}
