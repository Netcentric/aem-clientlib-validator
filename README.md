# AEM Clientlib FileVault Validator

[![Build Status](https://img.shields.io/github/actions/workflow/status/Netcentric/aem-content-package-namespace-validators/maven.yml?branch=main)](https://github.com/Netcentric/aem-content-package-namespace-validators/actions)
[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg)](https://opensource.org/licenses/EPL-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/biz.netcentric.filevault.validator/aem-content-package-namespace-validators)](https://central.sonatype.com/artifact/biz.netcentric.filevault.validator/aem-content-package-namespace-validators)
[![SonarCloud Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-content-package-namespace-validators&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Netcentric_aem-content-package-namespace-validators)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-content-package-namespace-validators&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Netcentric_aem-content-package-namespace-validators)

## Overview

Validates that FileVault content packages contain only Javascript files inside Client-Side libraries which can be minified without issues according to their (or the default) [minification configuration][clientlibs-jsprocessor].

It doesn't validate CSS files but only JS files. Also it only supports the processor with name `gcc` (backed by [Google Closure Compiler][google-closure-compiler].

## Implementation

This artifact provides a validator implementation for the [FileVault Validation Module][filevault-validation] and can be used for example with the [filevault-package-maven-plugin][filevault-p-m-p] like outlined below.
It tries to imitate the behavior of JS GCC minification at runtime (inside AEM). Directly using the AEM code is not possible as this part is closed source and only targeted towards running inside an OSGi runtime.

## Settings

The following options are supported apart from the default settings mentioned in [FileVault validation][filevault-validation].

Option | Default Value | Description | Since
 --- | --- | --- | ---
 `minJsProcessorConfiguration` | `min:gcc;languageIn=ECMASCRIPT_2018` | The default minification configuration used in the target AEM publish instance. If this value is set it must start with `min:`. Further information about this format in [Using Preprocessors][clientlibs-jsprocessor]. The value can be extracted from your AEM publish OSGi configuration for PID `com.adobe.granite.ui.clientlibs.impl.HtmlLibraryManagerImpl` from property `htmllibmanager.processor.js` | 1.0.0


## Fix Violations

Adjust either the HTML library configuration for the JS preprocessor (as outlined in [Using Preprocessors][clientlibs-jsprocessor]) or fix the JS file.

Some more detailed pointers are in the table below

Validation | Fix
--- | ---
`[JSC_LANGUAGE_FEATURE] This language feature is only supported for ECMASCRIPT_xxxx mode or better` | Make sure to adjust the client library's property `jsProcessor` with a suitable `languageIn` flag, like `"[min:gcc;languageIn=ECMASCRIPT_2015]"`
`

## Usage with Maven

You can use this validator with the [FileVault Package Maven Plugin][filevault-p-m-p] in version 1.4.0 or higher like this

```
<plugin>
  <groupId>org.apache.jackrabbit</groupId>
  <artifactId>filevault-package-maven-plugin</artifactId>
  <configuration>
    <validatorsSettings>
      <netcentric-clientlibrary-jsprocessor>
        <options>
          <!-- make sure to align with what is configured in your targeted AEM ->
          <minJsProcessorConfiguration>min:gcc;languageIn=ECMASCRIPT_2021</allowedPrincipalNamePatterns>
        </options>
      </netcentric-clientlibrary-jsprocessor>
    </validatorsSettings>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>biz.netcentric.filevault.validator</groupId>
      <artifactId>aem-clientlib-validator</artifactId>
      <version><latestversion></version>
    </dependency>
  </dependencies>
</plugin>
```

Adobe, and AEM are either registered trademarks or trademarks of Adobe in the United States and/or other countries.

[aemanalyser-maven-plugin]: https://github.com/adobe/aemanalyser-maven-plugin/tree/main/aemanalyser-maven-plugin
[filevault-validation]: https://jackrabbit.apache.org/filevault/validation.html
[filevault-p-m-p]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
[clientlibs-jsprocessor]: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/clientlibs#using-preprocessors
[google-closure-compiler]: https://developers.google.com/closure/compiler/
