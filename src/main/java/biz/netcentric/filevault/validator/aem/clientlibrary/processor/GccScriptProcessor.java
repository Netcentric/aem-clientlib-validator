/*-
 * #%L
 * AEM Clientlib FileVault Validator
 * %%
 * Copyright (C) 2026 Cognizant Netcentric
 * %%
 * All rights reserved. This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 * #L%
 */
package biz.netcentric.filevault.validator.aem.clientlibrary.processor;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.javascript.jscomp.AbstractCommandLineRunner;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SortingErrorManager;
import com.google.javascript.jscomp.SortingErrorManager.ErrorReportGenerator;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.jarjar.com.google.common.io.CharStreams;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A file processor that uses the Google Closure Compiler to validate JavaScript files.
 * Similar to the AEM internal implementation of bundle "com.adobe.granite.ui.clientlibs.processor.gcc" in version 1.0.30
 */
public class GccScriptProcessor implements ValidatingFileProcessor {
    private static final Logger log = LoggerFactory.getLogger(GccScriptProcessor.class);

    private static final Map<String, CompilationLevel> COMP_LEVELS = new HashMap<>();

    static {
        COMP_LEVELS.put("whitespace", CompilationLevel.WHITESPACE_ONLY);
        COMP_LEVELS.put("simple", CompilationLevel.SIMPLE_OPTIMIZATIONS);
        COMP_LEVELS.put("advanced", CompilationLevel.ADVANCED_OPTIMIZATIONS);
    }

    public Collection<ValidationMessage> process(
            @NotNull Path filePath,
            @NotNull Path basePath,
            @NotNull Reader sourceReader,
            @NotNull Map<String, String> flags)
            throws IOException {
        CompilerOptions options;
        // use custom error manager to pass errors/warnings with source path and line number to validation framework
        ValidatorErrorReportGenerator generator = new ValidatorErrorReportGenerator(filePath, basePath);
        Compiler compiler = new Compiler(new SortingErrorManager(Collections.singleton(generator)));
        options = createCompilerOptions(flags);
        SourceFile input = SourceFile.fromCode(filePath.toString(), CharStreams.toString(sourceReader));
        List<SourceFile> externs = AbstractCommandLineRunner.getBuiltinExterns(options.getEnvironment());
        compiler.compile(externs, Collections.singletonList(input), options);
        return generator.getValidationMessages();
    }

    private String getFlag(Map<String, String> flags, String name, String defaultValue) {
        String value = flags.get(name);
        return (value == null) ? defaultValue : value;
    }

    private CompilerOptions createCompilerOptions(Map<String, String> flags) {
        CompilerOptions options = new CompilerOptions();
        options.setLanguageIn(CompilerOptions.LanguageMode.valueOf(getFlag(flags, "languageIn", "ECMASCRIPT5")));
        options.setLanguageOut(CompilerOptions.LanguageMode.valueOf(getFlag(flags, "languageOut", "ECMASCRIPT5")));
        options.setStrictModeInput(Boolean.parseBoolean(getFlag(flags, "strictModeInput", "true")));
        String emitUseStrict = getFlag(flags, "emitUseStrict", null);
        if (emitUseStrict != null) options.setEmitUseStrict(Boolean.parseBoolean(emitUseStrict));
        CompilationLevel compLevel = COMP_LEVELS.get(getFlag(flags, "compilationLevel", "simple"));
        if (compLevel == null) {
            log.warn(
                    "unknown compilation level specified {}. fallback to 'simple'",
                    getFlag(flags, "compilationLevel", "simple"));
            compLevel = CompilationLevel.SIMPLE_OPTIMIZATIONS;
        }
        compLevel.setOptionsForCompilationLevel(options);
        options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF);
        options.setWarningLevel(DiagnosticGroups.MISPLACED_TYPE_ANNOTATION, CheckLevel.OFF);
        return options;
    }

    /*** deviations from AEM internal logic ***/
    static final class ValidatorErrorReportGenerator implements ErrorReportGenerator {

        private final Collection<ValidationMessage> validationMessages = new java.util.ArrayList<>();
        private final Path filePath;
        private final Path basePath;

        ValidatorErrorReportGenerator(Path filePath, Path basePath) {
            this.filePath = filePath;
            this.basePath = basePath;
        }

        @Override
        public void generateReport(SortingErrorManager manager) {
            for (JSError error : manager.getErrors()) {
                validationMessages.add(new ValidationMessage(
                        ValidationMessageSeverity.ERROR,
                        String.format("[%s] %s", error.getType().key, error.getDescription()),
                        filePath,
                        basePath,
                        error.getLineno(),
                        error.getCharno(),
                        null));
            }
        }

        public Collection<ValidationMessage> getValidationMessages() {
            return validationMessages;
        }
    }
}
