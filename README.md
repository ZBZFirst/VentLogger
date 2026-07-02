# VentLogger

VentLogger is a local-first Android charting companion for quickly recording
respiratory-care interaction details and reviewing them later for formal
documentation.

## Version

Current release target: `v1.5`

Version plan:

- `v1.5` through `v1.9`: first GitHub-hosted Android builds.
- `v2.0`: next major rollover after the `v1.x` line matures.

## Current Features

- Add/Edit patient templates with selectable respiratory charting categories.
- Log new timestamped entries from saved patient/category templates.
- Free-form `Comment` category.
- Attach gallery photos and videos to individual log entries.
- Review saved entries with sorting by patient, location, type, or time.
- Edit/delete mode in Review with confirmation for destructive deletes.
- View attached photos and play attached videos from the Review popup.
- Theme setting: system, light, or dark.
- Bulk CSV export from Settings:
  - one row per saved interaction
  - patient/location/type/timestamp metadata columns
  - one CSV column per charting group

## Privacy And Storage

VentLogger is local-only in this version. It does not sync to a server and does
not provide medical advice, clinical decision support, EHR integration, billing,
or coding automation.

Photo and video attachments are stored as Android content URI references tied to
the saved log entry.

## Build

From the project root:

```bash
./gradlew :app:compileDebugKotlin
./gradlew assembleDebug
```

The debug APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For GitHub distribution, attach the APK to a GitHub Release instead of
committing APK binaries into the source tree.

## GitHub Release Checklist

1. Verify compile:

   ```bash
   ./gradlew :app:compileDebugKotlin
   ```

2. Build APK:

   ```bash
   ./gradlew assembleDebug
   ```

3. Tag release:

   ```bash
   git tag -a v1.5 -m "VentLogger v1.5"
   ```

4. Push source and tag:

   ```bash
   git push -u origin main
   git push origin v1.5
   ```

5. Create a GitHub Release named `VentLogger v1.5` and attach:

   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
