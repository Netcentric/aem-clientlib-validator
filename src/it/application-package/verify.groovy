String buildLog = new File(basedir, 'build.log').text.normalize()

// application-package
assert buildLog.contains("""
[ERROR] ValidationViolation: [JSC_LANGUAGE_FEATURE] This language feature is only supported for ECMASCRIPT_2020 mode or better: Optional chaining. @ jcr_root${File.separator}apps${File.separator}mytenant${File.separator}clientlibrary1${File.separator}js${File.separator}editor${File.separator}editor.module.js, line 60, column 30, validator: netcentric-clientlibrary-jsprocessor
[ERROR] ValidationViolation: [JSC_LANGUAGE_FEATURE] This language feature is only supported for ECMASCRIPT_2020 mode or better: Optional chaining. @ jcr_root${File.separator}apps${File.separator}mytenant${File.separator}clientlibrary1${File.separator}js${File.separator}editor${File.separator}editor.module.js, line 61, column 30, validator: netcentric-clientlibrary-jsprocessor
[ERROR] ValidationViolation: [JSC_LANGUAGE_FEATURE] This language feature is only supported for ECMASCRIPT_2020 mode or better: Optional chaining. @ jcr_root${File.separator}apps${File.separator}mytenant${File.separator}clientlibrary1${File.separator}js${File.separator}editor${File.separator}editor.module.js, line 62, column 27, validator: netcentric-clientlibrary-jsprocessor
[ERROR] ValidationViolation: [JSC_LANGUAGE_FEATURE] This language feature is only supported for ECMASCRIPT_2020 mode or better: Optional chaining. @ jcr_root${File.separator}apps${File.separator}mytenant${File.separator}clientlibrary1${File.separator}js${File.separator}editor${File.separator}editor.module.js, line 63, column 27, validator: netcentric-clientlibrary-jsprocessor""") : 'application-package'

return true