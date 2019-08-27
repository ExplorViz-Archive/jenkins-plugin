package net.explorviz.jenkins.kieker.replay;

import net.explorviz.jenkins.kieker.configuration.KiekerConfiguration;
import teetime.framework.Execution;

import java.io.File;

public class RecordReplayer {
    private final ReplayConfiguration config;
    private Execution<ReplayConfiguration> execution;

    public RecordReplayer(File sourceDirectory, KiekerConfiguration kiekerConfig) {
        config = new ReplayConfiguration(new File[]{sourceDirectory}, kiekerConfig.readOnlyConfiguration());
    }

    public void run() {
        execution = new Execution<>(config);
        execution.executeNonBlocking();
    }

    public void kill() {
        if (execution != null) {
            execution.abortEventually();
        }
    }
}
