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
package biz.netcentric.filevault.validator.aem.clientlibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import biz.netcentric.filevault.validator.aem.clientlibrary.processor.GccScriptProcessor;
import biz.netcentric.filevault.validator.aem.clientlibrary.processor.ValidatingFileProcessor;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.vault.util.DocViewNode2;
import org.apache.jackrabbit.vault.util.DocViewProperty2;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.validation.spi.DocumentViewXmlValidator;
import org.apache.jackrabbit.vault.validation.spi.GenericJcrDataValidator;
import org.apache.jackrabbit.vault.validation.spi.NodeContext;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientLibraryJsProcessorValidator implements GenericJcrDataValidator, DocumentViewXmlValidator {

    private static final Name NAME_JS_PROCESSOR =
            NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "jsProcessor");

    private final ValidationMessageSeverity severity;
    private final Set<Path> relevantJsPaths = new java.util.HashSet<>();
    private final Map<String, ProcessorConfiguration> clientLibraryPathsAndProcessorConfiguration = new HashMap<>();
    private final ProcessorConfiguration defaultProcessorConfiguration;
    // keep insertion order to preserve the order of validation messages, as they are generated in the order of
    // processing
    private final Set<ValidationMessage> potentialValidationMessages = new LinkedHashSet<>();

    public ClientLibraryJsProcessorValidator(ValidationMessageSeverity severity, String defaultProcessorConfiguration) {
        super();
        this.severity = severity;
        this.defaultProcessorConfiguration = parseProcessorConfiguration(defaultProcessorConfiguration);
        if (!this.defaultProcessorConfiguration.getMode().equals("min")) {
            throw new IllegalArgumentException(String.format(
                    "Default processor configuration '%s' must start with prefix 'min:'.",
                    defaultProcessorConfiguration));
        }
    }

    @Override
    public @Nullable Collection<ValidationMessage> validate(
            @NotNull DocViewNode2 node, @NotNull NodeContext nodeContext, boolean isRoot) {
        if (isClientLibrary(node)) {
            ProcessorConfiguration processorConfig = defaultProcessorConfiguration;
            // capture the configured processor
            Optional<DocViewProperty2> property = node.getProperty(NAME_JS_PROCESSOR);
            if (property.isPresent()) {
                for (String processorConfigValue : property.get().getStringValues()) {
                    ProcessorConfiguration config = parseProcessorConfiguration(processorConfigValue);
                    if (config.getMode().equals("min")) {
                        processorConfig = config;
                    }
                }
            }
            // if the processor is set to "none", we don't need to validate the JS files
            if (!processorConfig.getName().equals("none")) {
                clientLibraryPathsAndProcessorConfiguration.putIfAbsent(
                        nodeContext.getNodePath(),
                        processorConfig); // store the processor configuration for this client library
            }
        }
        return null;
    }

    static ProcessorConfiguration parseProcessorConfiguration(String processorConfig) {
        String trimmedConfig = processorConfig.trim();
        int modeSeparatorIndex = trimmedConfig.indexOf(':');
        if (modeSeparatorIndex < 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid processor configuration format: '%s'", processorConfig));
        }

        String mode = trimmedConfig.substring(0, modeSeparatorIndex).trim();
        if (mode.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid processor configuration format: '%s'. Mode must not be empty!", processorConfig));
        }

        String remainder = trimmedConfig.substring(modeSeparatorIndex + 1).trim();

        String name;
        Map<String, String> options = new HashMap<>();

        int optionsSeparatorIndex = remainder.indexOf(';');
        if (optionsSeparatorIndex < 0) {
            name = remainder;
        } else {
            name = remainder.substring(0, optionsSeparatorIndex).trim();
            String optionsPart = remainder.substring(optionsSeparatorIndex + 1).trim();
            if (!optionsPart.isEmpty()) {
                for (String option : optionsPart.split(";")) {
                    String trimmedOption = option.trim();
                    if (trimmedOption.isEmpty()) {
                        continue;
                    }

                    int equalsIndex = trimmedOption.indexOf('=');
                    if (equalsIndex <= 0) {
                        continue;
                    }

                    String key = trimmedOption.substring(0, equalsIndex).trim();
                    String value = trimmedOption.substring(equalsIndex + 1).trim();
                    if (!key.isEmpty()) {
                        options.put(key, value);
                    }
                }
            }
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid processor configuration format: '%s'. Name must not be empty!", processorConfig));
        }

        return new ProcessorConfiguration(mode, name, options);
    }

    public static final class ProcessorConfiguration {
        private final @NotNull String mode;
        private final @NotNull String name;
        private final @NotNull Map<String, String> options;

        public ProcessorConfiguration(
                @NotNull String mode, @NotNull String name, @NotNull Map<String, String> options) {
            this.mode = mode;
            this.name = name;
            this.options = options;
        }

        public @NotNull String getMode() {
            return mode;
        }

        public @NotNull String getName() {
            return name;
        }

        public @NotNull Map<String, String> getOptions() {
            return options;
        }
    }

    boolean isClientLibrary(@NotNull DocViewNode2 node) {
        return node.getPrimaryType().orElse("").equals("cq:ClientLibraryFolder");
    }

    boolean isJsTxt(Path filePath) {
        // check if below client library folder there is a js.txt file, which contains the list of js files to be
        // included in the client library
        if (filePath.getFileName().equals(Paths.get("js.txt"))) {
            // verify this is below a client library node
            if (clientLibraryPathsAndProcessorConfiguration.containsKey(
                    getNodePathFromFilePath(filePath.getParent()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldValidateJcrData(@NotNull Path filePath, @NotNull Path basePath) {
        if (isJsTxt(filePath)) {
            return true;
        } else if (filePath.getFileName().toString().endsWith(".js")) {
            // potentially relevant JS
            return true;
        }
        return false;
    }

    private String getNodePathFromFilePath(Path filePath) {
        StringBuilder platformPathBuilder = new StringBuilder();
        filePath.iterator().forEachRemaining(pathElement -> {
            platformPathBuilder.append("/").append(pathElement.toString());
        });
        // String platformPath = FilenameUtils.separatorsToUnix(filePath.toString());
        return PlatformNameFormat.getRepositoryPath(platformPathBuilder.toString(), true);
    }

    @Override
    public @Nullable Collection<ValidationMessage> validateJcrData(
            @NotNull InputStream input,
            @NotNull Path filePath,
            @NotNull Path basePath,
            @NotNull Map<String, Integer> nodePathsAndLineNumbers)
            throws IOException {
        if (isJsTxt(filePath)) {
            relevantJsPaths.addAll(processJsTxtFile(input, filePath.getParent()));
        } else if (filePath.getFileName().toString().endsWith(".js")) {
            Optional<ProcessorConfiguration> processorConfiguration =
                    getClientLibraryProcessorConfigurationForJsFile(filePath);
            if (processorConfiguration.isEmpty()) {
                return java.util.List.of(new ValidationMessage(
                        ValidationMessageSeverity.DEBUG,
                        String.format(
                                "JavaScript file '%s' is not part of a client library folder and cannot be validated.",
                                filePath)));
            }
            // Perform validation for relevant JS files if needed
            Collection<ValidationMessage> messages =
                    validateJsFile(input, filePath, basePath, processorConfiguration.get());
            if (messages != null && !messages.isEmpty()) {
                if (relevantJsPaths.contains(filePath)) {
                    return messages;
                } else {
                    // we don't know yet if JS file is relevant, because js.txt file is not processed yet, so we will
                    // ignore the validation result for now
                    potentialValidationMessages.addAll(messages);
                }
            }
        }
        return null;
    }

    /**
     * This heuristics assumes the JS file is located inside a client library folder,
     * and tries to find the closest parent client library folder to get its processor configuration.
     * @param filePath the relative path of the JS file to be validated.
     * @return Optional containing the ProcessorConfiguration if found, or empty if not found.
     */
    private Optional<ProcessorConfiguration> getClientLibraryProcessorConfigurationForJsFile(@NotNull Path filePath) {
        String parentNodePath = getNodePathFromFilePath(filePath);
        ProcessorConfiguration processorConfiguration = clientLibraryPathsAndProcessorConfiguration.get(parentNodePath);
        if (processorConfiguration != null) {
            return Optional.of(processorConfiguration);
        } else if (filePath.getParent() != null) {
            // recursively check the parent directory for a client library folder
            return getClientLibraryProcessorConfigurationForJsFile(filePath.getParent());
        }
        return Optional.empty();
    }

    private @Nullable Collection<ValidationMessage> validateJsFile(
            @NotNull InputStream input,
            @NotNull Path filePath,
            @NotNull Path basePath,
            @NotNull ProcessorConfiguration processorConfig) {
        try {
            Optional<ValidatingFileProcessor> processor = getValidatingFileProcessor(processorConfig.getName());
            if (processor.isPresent()) {
                try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                    return processor.get().process(filePath, basePath, reader, processorConfig.getOptions());
                }
            } else {
                // don't warn for processor "none"
                if (!"none".equals(processorConfig.getName())) {
                    return java.util.List.of(new ValidationMessage(
                            ValidationMessageSeverity.WARN,
                            String.format(
                                    "JavaScript file could not be validated as processor '%s' is not supported.",
                                    processorConfig.getName()),
                            filePath,
                            basePath,
                            0,
                            0,
                            null));
                }
            }
        } catch (IOException e) {
            return java.util.List.of(new ValidationMessage(
                    severity,
                    String.format("Error validating JavaScript file: %s", e.getMessage()),
                    filePath,
                    basePath,
                    0,
                    0,
                    e));
        }
        return null;
    }

    static Optional<ValidatingFileProcessor> getValidatingFileProcessor(String processorName) {
        if ("gcc".equals(processorName)) {
            return Optional.of(new GccScriptProcessor());
        } else {
            return Optional.empty();
        }
    }

    static Set<Path> processJsTxtFile(@NotNull InputStream input, @NotNull Path filePath) throws IOException {
        return processJsTxtFile(new InputStreamReader(input, StandardCharsets.UTF_8), filePath);
    }

    static Set<Path> processJsTxtFile(@NotNull Reader reader, @NotNull Path bundleBasePath) throws IOException {
        Set<Path> paths = new java.util.HashSet<>();
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            Path basePath = bundleBasePath;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("#base=")) {
                    String newBasePath = PlatformNameFormat.getPlatformPath(line.substring(6));
                    if (!newBasePath.startsWith("/")) {
                        basePath = bundleBasePath.resolve(newBasePath);
                    } else {
                        basePath = Paths.get(newBasePath.substring(1));
                    }
                } else if (!line.startsWith("#")) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue; // skip empty lines
                    }
                    Path jsFilePath = basePath.resolve(line.trim());
                    paths.add(jsFilePath);
                }
            }
        }
        return paths;
    }

    @Override
    public @Nullable Collection<ValidationMessage> done() {
        Collection<ValidationMessage> relevantMessages = new LinkedHashSet<>();
        for (ValidationMessage message : potentialValidationMessages) {
            if (relevantJsPaths.contains(message.getFilePath())) {
                relevantMessages.add(message);
            }
        }
        return relevantMessages;
    }
}
