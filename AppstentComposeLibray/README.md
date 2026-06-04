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
