package net.explorviz.jenkins;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.util.FormValidation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Provides {@link FormValidation} shortcut methods that can be used like {@link FormValidation#validateRequired}.
 */
public final class FormValidationHelper {
    private FormValidationHelper() {
    }

    /**
     * Validate a given filepath form input. Makes sure a value is given (issuing an error otherwise, unless {@code
     * required} is {@code false}), and checks if the filepath points to an existing file, issuing a warning otherwise.
     *
     * @param project  Project, whose workspace is used as base directory to check relative filepaths.
     *                 The existence of the filepath is not validated if either this is {@code null} or
     *                 if the project doesn't have a build yet (as no workspace exists yet).
     * @param filepath The filepath that is expected to exist
     * @param required {@code true} if absence of the {@code filepath} should be considered an error, {@code false}
     *                 means the it is optional, but if specified, should still point to an existing file
     * @return {@link FormValidation}
     */
    public static FormValidation validateFilePath(@Nullable AbstractProject project, @Nullable String filepath,
                                                  boolean required) {
        if (required) {
            FormValidation validateRequired = FormValidation.validateRequired(filepath);
            /*
             * This is an ugly workaround, because we need to do additional tests in the OK case,
             * and we can't just inline #validateRequired because enforcer blocks us from accessing the
             * Messages class directly.
             */
            if (validateRequired.kind != FormValidation.Kind.OK) {
                return validateRequired;
            }
        }

        filepath = Util.fixEmptyAndTrim(filepath);
        /*
         * Project can be null if this Descriptor is not used from a project page.
         * Workspace can be null for projects that haven't been built yet.
         */
        if (filepath != null && project != null) {
            FilePath workspace = project.getSomeWorkspace();

            if (workspace != null) {
                FilePath child = workspace.child(filepath);
                try {
                    if (!child.exists()) {
                        return FormValidation.warning(Messages.FormValidationHelper_validateFilePath_doesNotExist());
                    } else if (child.isDirectory()) {
                        return FormValidation.error(Messages.FormValidationHelper_validateFilePath_isDirectory());
                    }
                } catch (IOException | InterruptedException e) {
                    return FormValidation.error(e, Messages.FormValidationHelper_validateFilePath_exception());
                }
            }
        }

        return FormValidation.ok();
    }

    /**
     * Validate a given text form input. Makes sure a value is given (issuing an error otherwise, unless {@code
     * required} is {@code false}), and checks if the string matches a given {@link Pattern}.
     *
     * @param value    The text input that should be validated
     * @param pattern  Pattern to test against
     * @param required {@code true} if absence of the {@code value} should be considered an error, {@code false} means
     *                 that it is optional, but if specified, must still match the given pattern
     * @return {@link FormValidation}
     */
    public static FormValidation validateString(@Nullable String value, @Nonnull Pattern pattern, boolean required) {
        if (required) {
            FormValidation validateRequired = FormValidation.validateRequired(value);
            if (validateRequired.kind != FormValidation.Kind.OK) {
                return validateRequired;
            }
        }

        value = Util.fixEmptyAndTrim(value);
        if (value != null) {
            if (!pattern.matcher(value).matches()) {
                return FormValidation.error(Messages.FormValidationHelper_validateString_doesNotMatch());
            }
        }

        return FormValidation.ok();
    }
}
