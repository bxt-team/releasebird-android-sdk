![image info](./imageAndroid.webp)
## Changelog

### 1.0.5

- Verified identities work: the hash passed to `identify(user, hash)` now reaches the widget (it was sent as `null` before).
- File attachments work: the file picker result is handed back to the widget.
- The widget no longer freezes on alert/confirm dialogs, and the back button closes it again.
- No more crashes when the widget settings miss a field, or when `identify`, `showWidget` or `logout` run before `init`.
- The launcher uses the colour and position (left/right) from the widget settings; hiding it with `showButton(false)` sticks.
- A message is shown and the widget closes when it cannot be loaded (e.g. offline).
- The anonymous id comes from a secure random generator; network calls have timeouts.
- The published POM lists the SDK's dependencies; EventBus, OkHttp and JUnit are no longer needed.
- The SDK no longer sets a `networkSecurityConfig` (this broke builds of apps with their own) and ships no launcher icons or generic resource names.
- New: `Releasebird.getInstance().logout()` and `showButton(boolean)`.
