# Migrating to AppstentCompose 1.10

## Resolve the JitPack artifact

Add `https://jitpack.io` to dependency repositories and use:

```kotlin
implementation("com.github.Appversation:AppstentCompose:v1.10.0-rc.2")
```

## Media3

AppstentCompose now uses AndroidX Media3 1.10.1. Applications that referenced
SDK implementation types must replace `com.google.android.exoplayer2.*`
imports with their `androidx.media3.*` equivalents. Applications that only
author ViewContent video descriptors do not need to change their JSON.

Review dependency constraints or exclusions that still force ExoPlayer 2;
running both families unnecessarily increases application size and can keep
obsolete integrations alive.

## Foundation Pager and Material

Tabs now use Compose Foundation Pager. Do not depend on Accompanist pager types
through the SDK. The SDK uses Material 3 for its outer theme and chrome but
retains selected Material 2 authored controls to preserve protected metrics;
those implementation types are not public API.

## Strict versioned descriptors

`schemaVersion: "1.0.0"` opts into recursive type-specific validation. Missing
or incorrectly typed required properties throw
`ViewContentDescriptorException`, whose `descriptorPath` identifies the
failure, for example `$.views[1].type`.

Unversioned documents remain permissive. Fix all AppstentAuthor diagnostics
before adding `schemaVersion`, then declare `minimumSDKVersions` and
`requiredCapabilities` for content that depends on 1.10 behavior.
