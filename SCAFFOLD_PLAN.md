# VentLogger Scaffold Plan

## Evidence From Current Project

- Project path: `/home/paulwasthere/AndroidStudioProjects/VentLogger`
- Gradle root name: `VentLogger`
- App module: `:app`
- Package/application id: `com.example.ventlogger`
- Compose enabled in `app/build.gradle.kts`
- Material3 adaptive navigation dependency is already present
- Current entry point: `app/src/main/java/com/example/ventlogger/MainActivity.kt`
- Current generated destinations: `HOME`, `FAVORITES`, `PROFILE`
- Groups-only hierarchy asset:
  `app/src/main/assets/respiratory_group_hierarchy.json`
- Source hierarchy:
  `/home/paulwasthere/Desktop/RespiratoryProject/label-reviewer/zebezo_respiratory_label_crosswalk_hierarchy_v3.json`

The source hierarchy contains the clinical group structure created for the
Respiratory Label Review project. VentLogger should use only the groups, not the
source value-label assignments. The Android asset is intentionally stripped down
to group `id`, `name`, `path`, and `children`.

## First Milestone

Convert the template navigation into a working VentLogger shell while keeping
the project compileable.

Destination mapping:

| Current | New | Purpose |
|---|---|---|
| `HOME` | `LOG` | Quick entry and current encounter work |
| `FAVORITES` | `REVIEW` | Saved encounters and charting summaries |
| `PROFILE` | `SETTINGS` | Local-only settings and safety boundary |

## Suggested First File Shape

Keep this small enough that Gemini can complete the first pass cleanly:

- `app/src/main/java/com/example/ventlogger/MainActivity.kt`
- `app/src/main/java/com/example/ventlogger/data/Encounter.kt`
- `app/src/main/java/com/example/ventlogger/data/EncounterRepository.kt`
- `app/src/main/java/com/example/ventlogger/data/RespiratoryGroup.kt`
- `app/src/main/java/com/example/ventlogger/data/RespiratoryGroupRepository.kt`
- `app/src/main/java/com/example/ventlogger/ui/LogScreen.kt`
- `app/src/main/java/com/example/ventlogger/ui/ReviewScreen.kt`
- `app/src/main/java/com/example/ventlogger/ui/SettingsScreen.kt`
- `app/src/main/java/com/example/ventlogger/ui/ChartingSummary.kt`

## MVP Behaviors

- Create a new encounter note
- Pick one or more respiratory groups from the hierarchy
- Add provider-entered values or notes under selected groups
- Save the encounter locally
- Show saved encounters newest-first
- Open an encounter for review
- Generate a plain-text charting summary
- Copy the charting summary to clipboard

## Encounter Fields

- `id`
- `patientIdentifier`
- `location`
- `encounterTime`
- `interactionType`
- `selectedGroupIds`
- `groupedNotes`
- `createdAt`
- `updatedAt`

## Charting Summary Format

```text
Time: [encounterTime]
Patient/Location: [patientIdentifier], [location]
Interaction: [interactionType]
Respiratory Groups: [selected group names]
Grouped Notes: [provider-entered grouped notes]
```

## Non-Goals For First Scaffold

- Importing or displaying the Respiratory Label Review `assigned_labels` values
- Generic charting fields such as Assessment, Interventions, Patient Response,
  Follow-up Plan, or Scratch Notes
- Network sync
- Cloud storage
- Login
- EHR integration
- Medical advice
- Clinical decision support
- Billing or coding automation

## Checkpoints

After any Gemini-generated changes, run:

```bash
cd /home/paulwasthere/AndroidStudioProjects/VentLogger
./gradlew :app:compileDebugKotlin
```

If that passes, run:

```bash
./gradlew assembleDebug
```
