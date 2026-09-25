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
import java.util.Map;

import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.jetbrains.annotations.NotNull;

public interface ValidatingFileProcessor {

    Collection<ValidationMessage> process(
            @NotNull Path filePath,
            @NotNull Path basePath,
            @NotNull Reader sourceReader,
            @NotNull Map<String, String> flags)
            throws IOException;
}
