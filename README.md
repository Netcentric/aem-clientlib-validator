# AEM Clientlib FileVault Validator

[![Build Status](https://img.shields.io/github/actions/workflow/status/Netcentric/aem-clientlib-validator/maven.yml?branch=main)](https://github.com/Netcentric/aem-clientlib-validator/actions)
[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg)](https://opensource.org/licenses/EPL-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/biz.netcentric.filevault.validator/aem-clientlib-validator)](https://central.sonatype.com/artifact/biz.netcentric.filevault.validator/aem-clientlib-validator)
[![SonarCloud Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-clientlib-validator&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Netcentric_aem-clientlib-validator)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Netcentric_aem-clientlib-validator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Netcentric_aem-clientlib-validator)

## Overview

Validates that FileVault content packages contain only Javascript files inside Client-Side libraries which can be minified without issues according to their (or the default) [minification configuration][clientlibs-jsprocessor].

It doesn't validate CSS files but only JS files. Also it only supports the processor with name `gcc` (backed by [Google Closure Compiler][google-closure-compiler]) but no longer the deprecated `yui` (backed by [YUI Compressor][yui-compressor]).

This validation is currently not performed by [aemanalyser-maven-plugin][aemanalyser-maven-plugin-issue].

## Implementation

This artifact provides a validator implementation for the [FileVault Validation Module][filevault-validation] and can be used for example with the [filevault-package-maven-plugin][filevault-p-m-p] like outlined below.
It replicates the behavior of JS GCC minification at runtime (inside AEM, performed by bundle `com.adobe.granite.ui.clientlibs.processor.gcc`). Directly using the AEM code is not possible as this part is closed source and only targeted towards running inside an OSGi runtime.

## Settings

The following options are supported apart from the default settings mentioned in [FileVault validation][filevault-validation].

Option | Default Value | Description | Since
 --- | --- | --- | ---
 `minJsProcessorConfiguration` | `min:gcc;languageIn=ECMASCRIPT_2018` | The default minification configuration used in the target AEM publish instance. This value must start with `min:`. Further information about this format in [Using Preprocessors][clientlibs-jsprocessor]. The value can be extracted from your AEM publish OSGi configuration for PID `com.adobe.granite.ui.clientlibs.impl.HtmlLibraryManagerImpl` from property `htmllibmanager.processor.js`. Only allows a single string value (not multiple ones, comma-separated). The value given here is only relevant for client libraries not having property [`jsProcessor`][clientlibs-jsprocessor]. | 1.0.0


## Fix Violations

Adjust either the HTML library configuration for the JS preprocessor (as outlined in [Using Preprocessors][clientlibs-jsprocessor]) or fix the JS file. In case both is not possible you can [suppress warnings/errors via annotations][gcc-suppress] as well.

Some more detailed pointers are in the table below

Validation | Fix
--- | ---
`[JSC_LANGUAGE_FEATURE] This language feature is only supported for ECMASCRIPT_xxxx mode or better` | Make sure to adjust the client library's property `jsProcessor` with a suitable `languageIn` flag, like `"[min:gcc;languageIn=ECMASCRIPT_2020]"`

Further information is contained in the [GCC documentation about warnings][gcc-warnings].


### Best practices for GCC minification settings

The documentation at [Using Client-Side Libraries on AEM as a Cloud Service](clientlibs-jsprocessor) is [incorrect](https://github.com/AdobeDocs/experience-manager-cloud-service.en/issues/346): By default only `languageIn` is set to `ECMASCRIPT_2018` and `languageOut` is still by default at `ECMASCRIPT5`.
The [defaults of Google Closure Compiler standalone][gcc-options] seem more reasonable but rely on placeholders (`STABLE`, `ECMASCRIPT_NEXT`) whose actual values depend on the GCC version, so Adobe updating GCC might impact the result when using those.
Therefore the recommendation is to reference only concrete and stable ES versions for `languageIn`.
AEMaaCS (since 2025.5 ships with GCC v20231112) and AEM 6.5 (ships with GCC v20210106) support the following values:

1. `ECMASCRIPT3`,
1. `ECMASCRIPT5`,
1. `ECMASCRIPT_2015`,
1. `ECMASCRIPT_2016`,
1. `ECMASCRIPT_2017`,
1. `ECMASCRIPT_2018`,
1. `ECMASCRIPT_2019`,
1. `ECMASCRIPT_2020`,
1. `ECMASCRIPT_2021` (only AEMaaCS, GCC v20231112),
1. `ECMASCRIPT_NEXT` (supported features depend on GCC version),

Apart from those some values whose actual meaning are GCC version specific like `STABLE`, `UNSTABLE`, `ECMASCRIPT_NEXT`, `UNSUPPORTED`.

 *The recommendation is to set flag `languageIn` and `languageOut` to the latest stable version which is `ECMASCRIPT_2021` (for AEMaaCS) or `ECMASCRIPT_2020` (for AEM 6.5 compatibility)*.

For further insights have a look at <https://github.com/google/closure-compiler/discussions/4346>. 

In cases where you minify at build time already you should completely *disable minification at client library level* by setting client library property `jsProcessor` to `min:none`.

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

It requires at least Java 17 to run.

Adobe, and AEM are either registered trademarks or trademarks of Adobe in the United States and/or other countries.

[aemanalyser-maven-plugin-issue]: https://github.com/adobe/aemanalyser-maven-plugin/issues/418
[filevault-validation]: https://jackrabbit.apache.org/filevault/validation.html
[filevault-p-m-p]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
[clientlibs-jsprocessor]: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/clientlibs#using-preprocessors
[google-closure-compiler]: https://developers.google.com/closure/compiler/
[gcc-options]: https://github.com/google/closure-compiler/wiki/Flags-and-Options
[yui-compressor]: https://yui.github.io/yuicompressor/
[gcc-warnings]: https://github.com/google/closure-compiler/wiki/Warnings
[gcc-suppress]: https://github.com/google/closure-compiler/wiki/@suppress-annotations
