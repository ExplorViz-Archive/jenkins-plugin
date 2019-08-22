package net.explorviz.jenkins;

import hudson.FilePath;
import hudson.Util;
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
     * @param workspace Base directory for relative filepaths. Can be {@code null} if no known workspace exists, in
     *                  which case the existence of the filepath isn't validated
     * @param filepath  The filepath that is expected to exist
     * @param required  {@code true} if absence of the {@code filepath} should be considered an error, {@code false}
     *                  means the it is optional, but if specified, should still point to an existing file
     * @return {@link FormValidation}
     */
    public static FormValidation validateFilePath(@Nullable FilePath workspace, @Nullable String filepath,
                                                  boolean required) {
        if (required) {
            FormValidation validateRequired = FormValidation.validateRequired(filepath);
            if (validateRequired.kind != FormValidation.Kind.OK) {
                return validateRequired;
            }
        }

        filepath = Util.fixEmptyAndTrim(filepath);
        // Workspace can be null for projects that haven't been built yet
        if (filepath != null && workspace != null) {
            FilePath child = workspace.child(filepath);
            try {
                if (!child.exists()) {
                    return FormValidation.warning("The given file path doesn't currently exist. " +
                        "Make sure it is available when this build step is run.");
                } else if (child.isDirectory()) {
                    return FormValidation.error("The given file path points to a directory. This is invalid.");
                }
            } catch (IOException | InterruptedException e) {
                return FormValidation.error(e, "Error trying to validate this file path. " +
                    "Make sure it is available when this build step is run.");
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
                return FormValidation.error("Given string does not match the required format! (See help)");
            }
        }

        return FormValidation.ok();
    }
}
