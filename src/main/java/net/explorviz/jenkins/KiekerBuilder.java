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
import kieker.common.util.filesystem.FSUtil;
import net.explorviz.jenkins.kiekerConfiguration.AbstractKiekerConfiguration;
import net.explorviz.jenkins.kiekerConfiguration.FileWriterConfiguration;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
    private final String runId;
    private final String appJar;
    private final String aopXml;
    private final int executeDuration;

    // Optional arguments
    private String runName;
    private String appArgs;
    private boolean skipDefaultAOP;
    private boolean failBuildOnEmpty;

    // Optional arguments listed under "Advanced"
    private String vmOpts;
    private String kiekerJar;
    private String kiekerOverrides;

    @DataBoundConstructor
    public KiekerBuilder(@Nonnull String runId, @Nonnull String appJar, @Nonnull String aopXml,
                         @Nonnegative int executeDuration) {
        this.runId = runId;
        this.appJar = appJar;
        this.aopXml = aopXml;
        this.executeDuration = executeDuration;

        // Must match defaults in KiekerBuilder/config.jelly
        this.appArgs = null;
        this.skipDefaultAOP = true;
        this.failBuildOnEmpty = true;

        this.vmOpts = null;
        this.kiekerJar = null;
        this.kiekerOverrides = null;
    }

    public String getRunId() {
        return runId;
    }

    public String getAppJar() {
        return appJar;
    }

    public int getExecuteDuration() {
        return executeDuration;
    }

    public String getRunName() {
        return runName;
    }

    @DataBoundSetter
    public void setRunName(@Nullable String runName) {
        this.runName = runName;
    }

    public String getAppArgs() {
        return appArgs;
    }

    @DataBoundSetter
    public void setAppArgs(@Nullable String appArgs) {
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
    public void setVmOpts(@Nullable String vmOpts) {
        this.vmOpts = vmOpts;
    }

    public String getKiekerJar() {
        return kiekerJar;
    }

    @DataBoundSetter
    public void setKiekerJar(@Nullable String kiekerJar) {
        this.kiekerJar = kiekerJar;
    }

    public String getKiekerOverrides() {
        return kiekerOverrides;
    }

    @DataBoundSetter
    public void setKiekerOverrides(@Nullable String kiekerOverrides) {
        this.kiekerOverrides = kiekerOverrides;
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
        if (Util.fixEmptyAndTrim(runId) == null) {
            listener.fatalError("Instrumentation ID is required! Failing build.");
            run.setResult(Result.FAILURE);
            return;
        }

        /*
         * We create a working directory with the prefix "kieker" and the run id that will contain all the files that
         * we need to operate, as well as the record logs.
         */
        FilePath workingDirectory = workspace.child("kieker." + runId);
        if (workingDirectory.exists()) {
            listener.fatalError(
                "Instrumentation with ID '%s' already exists in workspace! Not overriding implicitly, failing build.",
                runId);
            run.setResult(Result.FAILURE);
            return;
        }
        workingDirectory.mkdirs();

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
            listener.error("No AspectJ weaving configuration file specified or does not exist!");
        } else {
            // AspectJ will silently stop working when using absolute paths, hence we can not use FilePaths here
            args.add(ARG_ASPECTJ_WEAVER_CONFIGURATION + aopXml);
        }

        /*
         * Kieker monitoring configuration
         */
        FileWriterConfiguration monitoringConfiguration = new FileWriterConfiguration();
        monitoringConfiguration.setApplicationName(run.getParent().getName() + "_" + run.getId() + "_" + runId);
        monitoringConfiguration.setStoragePath(workingDirectory.getRemote());

        if (Util.fixEmptyAndTrim(kiekerOverrides) != null) {
            monitoringConfiguration.getConfiguration().load(new StringReader(kiekerOverrides));
        }

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

        if (Util.fixEmptyAndTrim(appJar) == null || !workspace.child(appJar).exists()) {
            listener.error("No application jar file specified or does not exist! Failing build.");
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
        if (kiekerProc.joinWithTimeout(60, TimeUnit.SECONDS, listener) != 0) {
            listener.getLogger().println("Killed application after reaching instrumentation duration");
        }

        /*
         * Collect results
         */
        Optional<FilePath> recordDir =
            workingDirectory.listDirectories().stream().filter(KiekerBuilder::isKiekerDirectory).findFirst();
        if (recordDir.isPresent()) {
            listener.getLogger().println("Kieker records were saved to: " + recordDir.get().getRemote());
            // TODO: Archive as artifacts: run.getArtifactManager().archive(recordDir.get(), launcher, listener, map);
            run.addAction(new ExplorVizAction(runId, runName, recordDir.get().getName()));
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
            return !path.list(new KiekerDirectoryFilter()).isEmpty();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * The FileFilter is possibly run on a remote agent and therefore needs to be Serializable
     */
    private static class KiekerDirectoryFilter implements FileFilter, Serializable {
        private static final long serialVersionUID = -5474218848206939281L;

        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(FSUtil.MAP_FILE_EXTENSION);
        }
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Symbol("kieker")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private static final Pattern RUN_ID_PATTERN = Pattern.compile("[a-z0-9_\\-]{1,64}");

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "ExplorViz: Run Kieker instrumentation";
        }

        public FormValidation doCheckRunId(@QueryParameter String value) {
            return FormValidationHelper.validateString(value, RUN_ID_PATTERN, true);
        }

        public FormValidation doCheckRunName(@QueryParameter String value) {
            return FormValidation.ok();
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

        public FormValidation doCheckExecuteDuration(@QueryParameter int value) {
            if (value <= 0) {
                return FormValidation.error("Duration must be a positive number!");
            }

            return FormValidation.ok();
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

        public FormValidation doCheckKiekerOverrides(@QueryParameter String value) {
            Properties properties = new Properties();
            try {
                properties.load(new StringReader(value));
            } catch (IOException e) {
                return FormValidation.error(e, "Could not read properties");
            }

            Collection<FormValidation> warnings = new HashSet<>(0);
            for (String key : properties.stringPropertyNames()) {
                if (!key.startsWith(AbstractKiekerConfiguration.PROPS_PREFIX)) {
                    warnings.add(FormValidation
                        .warning("Property '%s' does not start with '%s'. It will likely have no effect.", key,
                            AbstractKiekerConfiguration.PROPS_PREFIX));
                }
                if (key.equalsIgnoreCase(AbstractKiekerConfiguration.PROP_WRITER)) {
                    warnings.add(
                        FormValidation.error("Overriding '%s' is not supported and will most likely break the plugin!",
                            AbstractKiekerConfiguration.PROP_WRITER));
                }
                if (key.equalsIgnoreCase(FileWriterConfiguration.PROP_STORAGE_PATH)) {
                    warnings.add(
                        FormValidation.error("Overriding '%s' is not supported and will most likely break the plugin!",
                            FileWriterConfiguration.PROP_STORAGE_PATH));
                }
            }

            return FormValidation.aggregate(warnings);
        }
    }
}
