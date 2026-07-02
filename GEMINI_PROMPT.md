# Gemini Prompt

Use this prompt inside Android Studio with the existing project open:

```text
We are working in the existing Android project:
/home/paulwasthere/AndroidStudioProjects/VentLogger

The project already has a Kotlin Jetpack Compose template with Material3 adaptive navigation in:
app/src/main/java/com/example/ventlogger/MainActivity.kt

Keep the current package/application id for now: com.example.ventlogger.

Goal:
Turn this template into the first compileable MVP of VentLogger, a local-first app where a healthcare provider can quickly enter patient-interaction notes during care and later reference those notes while charting at a computer terminal.

Important domain scaffold:
Use the groups-only respiratory hierarchy asset already added at:
app/src/main/assets/respiratory_group_hierarchy.json

That asset is derived from the Respiratory Label Review hierarchy:
/home/paulwasthere/Desktop/RespiratoryProject/label-reviewer/zebezo_respiratory_label_crosswalk_hierarchy_v3.json

Use the hierarchy groups as clinical charting categories/sections. Do not import or recreate the original assigned value labels. Providers should enter their own values, observations, and notes under those groupings.

Use the existing navigation UI instead of replacing the whole app. Rename/rework the three generated destinations:
- Home -> Log
- Favorites -> Review
- Profile -> Settings

Build the first local-only MVP:
- Encounter list
- New encounter entry form
- Encounter detail/review view
- Respiratory group/category selection from respiratory_group_hierarchy.json
- Grouped observations or notes attached to an encounter
- Generated charting summary text
- Copy charting summary to clipboard
- In-memory or simple local persistence is acceptable for the first pass, but keep the data layer easy to replace with Room later

Data model:
- id
- patientIdentifier
- location
- encounterTime
- interactionType
- selectedGroups
- groupedNotes
- createdAt
- updatedAt

Do not add generic charting text fields such as Assessment, Interventions, Patient Response, Follow-up Plan, or Scratch Notes. The log surface should contain only the selected hierarchy groups/items and provider-entered values/notes for those selected items.

Safety boundary:
- No network features
- No cloud sync
- No authentication yet
- No EHR integration
- No medical advice generation
- No clinical decision support

Keep the app compileable after each step.
First verification command:
./gradlew :app:compileDebugKotlin
```
```
