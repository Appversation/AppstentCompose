# AppstentCompose

## Content Environments

Configure the API key and optional content environment before loading remote Appstent content:

```kotlin
ModuleConfigs.apiKey = "<appstent-api-key>"
ModuleConfigs.contentEnvironment = "prod" // or "qa", "staging_content", etc.
```

`contentEnvironment` defaults to `prod`. Environment IDs are normalized to lowercase and must use 1-63 letters, numbers, underscores, or hyphens.

To validate the selected environment explicitly:

```kotlin
val repository = ViewContentRepository()
val result = repository.validateContentEnvironment()

if (!result.isValid) {
    // result.statusCode and result.message include the backend response.
}
```

Content requests throw `ViewContentRequestException` for non-2xx backend responses, including inactive or unknown content environments.

## Design Tokens

Content environments can have one active DTCG design token file. Load it before rendering tokenized content:

```kotlin
ModuleConfigs.apiKey = "<appstent-api-key>"
ModuleConfigs.contentEnvironment = "qa"

val repository = ViewContentRepository()
repository.loadActiveDesignTokens()
```

The renderer resolves exact braced token references such as `{color.text.primary}` in color, dimension, font, progress, shadow, spacing, and frame fields. Missing or invalid token references fall back to the original content value so existing content keeps rendering.
