package net.explorviz.jenkins.kiekerReplay;

import kieker.analysis.sink.DataSinkStage;
import kieker.analysis.source.file.DirectoryReaderStage;
import kieker.analysis.source.file.DirectoryScannerStage;
import teetime.framework.Configuration;

import java.io.File;

/**
 * TeeTime stage configuration that configures the following steps:
 * <ul>
 *     <li>Recursively scan the given directories for kieker record folders</li>
 *     <li>Read the records from the folders found</li>
 *     <li>Process them using Kieker, configured via the given kieker configuration
 *         (in most cases you will want to write them to another sink, e.g. TCP)</li>
 * </ul>
 */
public class ReplayConfiguration extends Configuration {
    public ReplayConfiguration(File[] directories, kieker.common.configuration.Configuration kiekerConfg) {
        super();

        final DirectoryScannerStage directoryScannerStage = new DirectoryScannerStage(directories);
        final DirectoryReaderStage directoryReaderStage = new DirectoryReaderStage(kiekerConfg);
        final DataSinkStage dataSinkStage = new DataSinkStage(kiekerConfg);

        this.connectPorts(directoryScannerStage.getOutputPort(), directoryReaderStage.getInputPort());
        this.connectPorts(directoryReaderStage.getOutputPort(), dataSinkStage.getInputPort());
    }
}
