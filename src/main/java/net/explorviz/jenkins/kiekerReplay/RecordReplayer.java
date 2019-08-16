package net.explorviz.jenkins.kiekerReplay;

import net.explorviz.jenkins.kiekerConfiguration.KiekerConfiguration;
import teetime.framework.Execution;

import java.io.File;

public class RecordReplayer {
    private final ReplayConfiguration config;
    private Execution<ReplayConfiguration> execution = null;

    public RecordReplayer(File sourceDirectory, KiekerConfiguration kiekerConfig) {
        config = new ReplayConfiguration(new File[]{sourceDirectory}, kiekerConfig.readOnlyConfiguration());
    }

    public void run() {
        execution = new Execution<>(config);
        execution.executeNonBlocking();
    }

    public void kill() {
        execution.abortEventually();
    }
}
