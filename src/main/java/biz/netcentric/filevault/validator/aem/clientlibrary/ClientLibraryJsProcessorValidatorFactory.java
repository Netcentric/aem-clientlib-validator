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

import org.apache.jackrabbit.vault.validation.spi.ValidationContext;
import org.apache.jackrabbit.vault.validation.spi.Validator;
import org.apache.jackrabbit.vault.validation.spi.ValidatorFactory;
import org.apache.jackrabbit.vault.validation.spi.ValidatorSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.MetaInfServices;

@MetaInfServices(ValidatorFactory.class)
public class ClientLibraryJsProcessorValidatorFactory implements ValidatorFactory {

    @Override
    public @Nullable Validator createValidator(
            @NotNull ValidationContext context, @NotNull ValidatorSettings settings) {
        return new ClientLibraryJsProcessorValidator(
                settings.getDefaultSeverity(),
                settings.getOptions()
                        .getOrDefault("minJsProcessorConfiguration", "min:gcc;languageIn=ECMASCRIPT_2018"));
    }

    @Override
    public boolean shouldValidateSubpackages() {
        return false;
    }

    @Override
    public @NotNull String getId() {
        return "netcentric-clientlibrary-jsprocessor";
    }

    @Override
    public int getServiceRanking() {
        return 0;
    }
}
