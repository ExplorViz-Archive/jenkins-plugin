package net.explorviz.jenkins.kiekerReplay;

import kieker.common.configuration.Configuration;
import teetime.framework.Execution;

import java.io.File;

public class RecordReplayer {
    private final ReplayConfiguration config;
    private Execution<ReplayConfiguration> execution = null;

    public RecordReplayer(File sourceDirectory, Configuration kiekerConfig) {
        config = new ReplayConfiguration(new File[]{sourceDirectory}, kiekerConfig);
    }

    public void run() {
        execution = new Execution<>(config);
        execution.executeNonBlocking();
    }

    public void kill() {
        execution.abortEventually();
    }
}
