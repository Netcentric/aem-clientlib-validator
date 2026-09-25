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

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientLibraryJsProcessorValidatorTest {

    @Test
    void processJsTxtFileWithReaderResolvesRelativeAndAbsoluteBasePaths() throws IOException {
        String jsTxtContent = """
                #base=js

                main.js
                deeply/nested.js
                #base=/apps/shared/clientlib
                vendor.js
                # this is a comment"
                """;

        Path bundleBasePath = Paths.get("apps/project/clientlib");

        Set<Path> result =
                ClientLibraryJsProcessorValidator.processJsTxtFile(new StringReader(jsTxtContent), bundleBasePath);

        assertThat(result)
                .containsExactlyInAnyOrder(
                        Paths.get("apps", "project", "clientlib", "js", "main.js"),
                        Paths.get("apps", "project", "clientlib", "js", "deeply", "nested.js"),
                        Paths.get("apps", "shared", "clientlib", "vendor.js"));
    }

    @Test
    void parseProcessorConfigurationWithSimpleFormat() {
        String processorConfig = "min:gcc";

        ClientLibraryJsProcessorValidator.ProcessorConfiguration config =
                ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig);

        assertThat(config.getMode()).isEqualTo("min");
        assertThat(config.getName()).isEqualTo("gcc");
        assertThat(config.getOptions()).isEmpty();
    }

    @Test
    void parseProcessorConfigurationWithWhitespace() {
        String processorConfig = "  min  :  gcc  ";

        ClientLibraryJsProcessorValidator.ProcessorConfiguration config =
                ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig);

        assertThat(config.getMode()).isEqualTo("min");
        assertThat(config.getName()).isEqualTo("gcc");
        assertThat(config.getOptions()).isEmpty();
    }

    @Test
    void parseProcessorConfigurationWithOptions() {
        String processorConfig = "min:gcc;optionA=valueA;optionB=valueB";

        ClientLibraryJsProcessorValidator.ProcessorConfiguration config =
                ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig);

        assertThat(config.getMode()).isEqualTo("min");
        assertThat(config.getName()).isEqualTo("gcc");
        assertThat(config.getOptions())
                .containsExactlyInAnyOrderEntriesOf(Map.of("optionA", "valueA", "optionB", "valueB"));
    }

    @Test
    void parseProcessorConfigurationWithOptionsAndWhitespace() {
        String processorConfig = "min : yui ; level = 2 ; optimize = true ";

        ClientLibraryJsProcessorValidator.ProcessorConfiguration config =
                ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig);

        assertThat(config.getMode()).isEqualTo("min");
        assertThat(config.getName()).isEqualTo("yui");
        assertThat(config.getOptions()).containsExactlyInAnyOrderEntriesOf(Map.of("level", "2", "optimize", "true"));
    }

    @Test
    void parseProcessorConfigurationWithTrailingSemicolons() {
        String processorConfig = "min:gcc;;optionA=valueA;;";

        ClientLibraryJsProcessorValidator.ProcessorConfiguration config =
                ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig);

        assertThat(config.getMode()).isEqualTo("min");
        assertThat(config.getName()).isEqualTo("gcc");
        assertThat(config.getOptions()).containsExactlyInAnyOrderEntriesOf(Map.of("optionA", "valueA"));
    }

    @Test
    void parseProcessorConfigurationWithMalformedOptions() {
        // Options without '=' are skipped
        String processorConfig = "min:gcc;optionNoEquals;optionA=valueA";

        ClientLibraryJsProcessorValidator.ProcessorConfiguration config =
                ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig);

        assertThat(config.getOptions()).containsExactlyInAnyOrderEntriesOf(Map.of("optionA", "valueA"));
    }

    @Test
    void parseProcessorConfigurationWithEmptyOptionKey() {
        // Options with empty key are skipped
        String processorConfig = "min:gcc;=value;optionA=valueA";

        ClientLibraryJsProcessorValidator.ProcessorConfiguration config =
                ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig);

        assertThat(config.getOptions()).containsExactlyInAnyOrderEntriesOf(Map.of("optionA", "valueA"));
    }

    @Test
    void parseProcessorConfigurationThrowsExceptionWhenMissingSeparator() {
        String processorConfig = "mingcc"; // missing colon

        assertThatThrownBy(() -> ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid processor configuration format");
    }

    @Test
    void parseProcessorConfigurationThrowsExceptionWhenMissingMode() {
        String processorConfig = ":gcc"; // missing mode

        assertThatThrownBy(() -> ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid processor configuration format");
    }

    @Test
    void parseProcessorConfigurationThrowsExceptionWhenMissingName() {
        String processorConfig = "min:"; // missing name

        assertThatThrownBy(() -> ClientLibraryJsProcessorValidator.parseProcessorConfiguration(processorConfig))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid processor configuration format");
    }

    @Test
    void processJsTxtFileSkipsEmptyLines() throws IOException {
        String jsTxtContent = """
                #base=js
                main.js


                vendor.js
                """;

        Path bundleBasePath = Paths.get("apps/project/clientlib");

        Set<Path> result =
                ClientLibraryJsProcessorValidator.processJsTxtFile(new StringReader(jsTxtContent), bundleBasePath);

        assertThat(result)
                .containsExactlyInAnyOrder(
                        Paths.get("apps", "project", "clientlib", "js", "main.js"),
                        Paths.get("apps", "project", "clientlib", "js", "vendor.js"));
    }

    @Test
    void processJsTxtFileSkipsCommentLines() throws IOException {
        String jsTxtContent = """
                #base=js
                # This is a comment
                main.js
                # Another comment with special chars: !@#$%^
                vendor.js
                """;

        Path bundleBasePath = Paths.get("apps/project/clientlib");

        Set<Path> result =
                ClientLibraryJsProcessorValidator.processJsTxtFile(new StringReader(jsTxtContent), bundleBasePath);

        assertThat(result)
                .containsExactlyInAnyOrder(
                        Paths.get("apps", "project", "clientlib", "js", "main.js"),
                        Paths.get("apps", "project", "clientlib", "js", "vendor.js"));
    }
}
