package net.explorviz.jenkins;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.QuotedStringTokenizer;
import jenkins.FilePathFilter;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import kieker.common.util.filesystem.FSUtil;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class KiekerBuilder extends Builder implements SimpleBuildStep {
    private static final String ARG_JAVA_AGENT = "-javaagent:";
    private static final String ARG_JAR = "-jar";
    private static final String ARG_KIEKER_MONITORING_CONFIGURATION = "-Dkieker.monitoring.configuration=";
    private static final String ARG_ASPECTJ_WEAVER_CONFIGURATION = "-Dorg.aspectj.weaver.loadtime.configuration=";
    private static final String ARG_SKIP_DEFAULT_AOP_CONFIGURATION =
        "-Dkieker.monitoring.skipDefaultAOPConfiguration=true";

    private static final String BUILTIN_KIEKER_JAR = "kieker-1.14-SNAPSHOT-aspectj.jar";

    // Required arguments
    private final String appJar;
    private final String aopXml;

    // Optional arguments
    private String appArgs;
    private boolean skipDefaultAOP;
    private boolean failBuildOnEmpty;

    // Optional arguments listed under "Advanced"
    private String vmOpts;
    private String kiekerJar;
    private String kiekerProperties;

    @DataBoundConstructor
    public KiekerBuilder(String appJar, String aopXml) {
        this.appJar = appJar;
        this.aopXml = aopXml;

        // Must match defaults in KiekerBuilder/config.jelly
        this.appArgs = null;
        this.skipDefaultAOP = true;
        this.failBuildOnEmpty = true;

        this.vmOpts = null;
        this.kiekerJar = null;
        this.kiekerProperties = null;
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

    public String getKiekerProperties() {
        return kiekerProperties;
    }

    @DataBoundSetter
    public void setKiekerProperties(String kiekerProperties) {
        this.kiekerProperties = kiekerProperties;
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
        /*
         * We create a temporary directory with the prefix "kieker" that will contain all the files that we need to
         * operate, as well as the record logs.
         */
        FilePath workingDirectory = workspace.createTempDir("kieker", null);

        ArgumentListBuilder args = new ArgumentListBuilder();

        /*
         * JDK
         * TODO: Make JDK selectable
         */
        args.add(Jenkins.get().getJDKs().get(0).getBinDir() + "/java");

        /*
         * Java agent
         */
        FilePath javaagent;
        String trimmedKiekerJar = Util.fixEmptyAndTrim(kiekerJar);
        // When the user doesn't specify a kieker jar to use...
        if (trimmedKiekerJar == null) {
            javaagent = workingDirectory.child(BUILTIN_KIEKER_JAR);

            // ... we copy our own, built-in version of kieker to our working directory
            try (InputStream in = KiekerBuilder.class.getClassLoader().getResourceAsStream(BUILTIN_KIEKER_JAR);
                 OutputStream out = javaagent.write()) {
                if (in == null) {
                    throw new IOException("Built-in kieker jar '" + BUILTIN_KIEKER_JAR + "' not accessible");
                }

                IOUtils.copy(in, out);
            }
        } else {
            javaagent = workspace.child(trimmedKiekerJar);

            // Make sure the user-specified kieker jar exists, because it is required
            if (!javaagent.exists()) {
                listener.fatalError("Specified kieker jar '%s' does not exist!", trimmedKiekerJar);
                run.setResult(Result.FAILURE);
                return;
            }
        }

        args.add(ARG_JAVA_AGENT + javaagent.getRemote());

        /*
         * AspectJ weaver configuration
         */
        if (Util.fixEmptyAndTrim(aopXml) == null || !workspace.child(aopXml).exists()) {
            // We do not abort the build here, because the aspectj file might be optional in some situations
            // (although UI will always claim it is required)
            listener.error("No AspectJ weaving configuration file given or does not exist!");
        } else {
            // AspectJ will silently stop working when using absolute paths, hence we can not use FilePaths here
            args.add(ARG_ASPECTJ_WEAVER_CONFIGURATION + aopXml);
        }

        /*
         * Kieker monitoring configuration
         */
        KiekerMonitoringConfiguration monitoringConfiguration = new KiekerMonitoringConfiguration();
        monitoringConfiguration.setApplicationName(run.getParent().getFullDisplayName() + "_" + run.getId());
        monitoringConfiguration.setOutputDirectory(workingDirectory.getRemote());
        // TODO: Implement kieker configuration overrides

        FilePath monitoringConfigurationFile = workingDirectory.child("kieker.monitoring.configuration");
        monitoringConfiguration.write(monitoringConfigurationFile);

        args.add(ARG_KIEKER_MONITORING_CONFIGURATION + monitoringConfigurationFile.getRemote());

        if (skipDefaultAOP) {
            args.add(ARG_SKIP_DEFAULT_AOP_CONFIGURATION);
        }

        /*
         * Other options
         */
        if (Util.fixEmptyAndTrim(vmOpts) != null) {
            args.add(QuotedStringTokenizer.tokenize(vmOpts));
        }

        if (!workspace.child(appJar).exists()) {
            listener.error("Specified application jar file '%s' does not exist! Failing build.", appJar);
            run.setResult(Result.FAILURE);
            return;
        }
        args.add(ARG_JAR, appJar);

        if (Util.fixEmptyAndTrim(appArgs) != null) {
            args.add("--");
            args.add(QuotedStringTokenizer.tokenize(appArgs));
        }

        /*
         * Start run with kieker
         */
        Proc kiekerProc = launcher.launch().stdout(listener).pwd(workspace).cmds(args).start();
        // TODO: Configurable test time, don't fail build when hitting timeout
        if (kiekerProc.joinWithTimeout(60, TimeUnit.SECONDS, listener) != 0) {
            run.setResult(Result.FAILURE);
        }

        /*
         * Collect results
         */
        Optional<FilePath> recordDir =
            workingDirectory.listDirectories().stream().filter(KiekerBuilder::isKiekerDirectory).findFirst();
        if (recordDir.isPresent()) {
            listener.getLogger().println("Kieker records were saved to: " + recordDir.get().getRemote());
            // TODO: Archive as artifacts: run.getArtifactManager().archive(recordDir.get(), launcher, listener, map);
            run.addAction(new ExplorVizAction(recordDir.get().getName()));
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
            return !path
                // The FileFilter is possibly run on a remote agent and needs to be Serializable;
                // this trick convinces javac to make the lambda implement Serializable
                .list((FileFilter & Serializable) pathname -> pathname.getName().endsWith(FSUtil.MAP_FILE_EXTENSION))
                .isEmpty();
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
            // TODO: Only allow one single Kieker build step for now
            return true;
        }

        @Override
        public String getDisplayName() {
            return "ExplorViz: Run Kieker instrumentation";
        }

        public FormValidation doCheckAppJar(@QueryParameter String value, @AncestorInPath AbstractProject project) {
            return FormValidationHelper.validateFilePath(project.getSomeWorkspace(), value, true);
        }

        public FormValidation doCheckAppArgs(@QueryParameter String value) {
            return FormValidation.ok();
        }

        public FormValidation doCheckAopXml(@QueryParameter String value, @AncestorInPath AbstractProject project) {
            return FormValidationHelper.validateFilePath(project.getSomeWorkspace(), value, true);
        }

        public FormValidation doCheckVmOpts(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.ok();
            }

            Collection<FormValidation> warnings = new HashSet<>(0);

            if (value.contains(ARG_JAVA_AGENT)) {
                warnings.add(
                    FormValidation.warning("Do not specify %s! It is set to kieker automatically.", ARG_JAVA_AGENT));
            }
            if (value.contains(ARG_JAR)) {
                warnings.add(FormValidation.warning("Do not specify %s! It is set automatically.", ARG_JAR));
            }
            if (value.contains(ARG_KIEKER_MONITORING_CONFIGURATION)) {
                warnings.add(FormValidation.error("Do not specify the kieker monitoring configuration! " +
                    "It is auto-generated and other configurations won't work."));
            }
            if (value.contains(ARG_ASPECTJ_WEAVER_CONFIGURATION)) {
                warnings.add(FormValidation.warning("Do not specify the AspectJ weaver configuration here! " +
                    "Use the form input above instead."));
            }

            return FormValidation.aggregate(warnings);
        }

        public FormValidation doCheckKiekerJar(@QueryParameter String value, @AncestorInPath AbstractProject project) {
            return FormValidationHelper.validateFilePath(project.getSomeWorkspace(), value, false);
        }

        public FormValidation doCheckKiekerProperties(@QueryParameter String value) {
            // TODO: Warn if a line does not start with "kieker.monitoring"
            return FormValidation.ok();
        }
    }
}
