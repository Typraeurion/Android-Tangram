# Preparing for the Next Release

- [ ] Run all tests
    - Open a new sheet in `doc/Test_Results.ods`.  Copy or create a matrix of test classes vs. platforms.
    - Run all tests under `app/src/test/` stand-alone.  Enter the results in the spreadsheet in the “N/A” platform column (“Passed” or “'passed/total” for any failures)
    - For each desired target for API’s ≥ `minSdk`,
        1. Start the emulator; wait for it to be available in the “Running Devices” drop-down (select it if necessary).
        2. Run “TangramPuzzleTests”.  Enter the results.
        3. Stop the emulator.
    - Address any failing tests as you go.  All stand-alone tests under `app/src/test/` **must** pass.  All instrumented tests under `app/src/androidTest` _should_ pass; any exceptions must be for good reason and documented.
- [ ] Check the project for lint errors
    1. `./gradlew lint`
    2. Check the results in `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt` for any “Error”s.
    3. Fix the errors as needed.
- [ ] Increment the application version to the next applicable number (minor for added features, patch for bug fixes).
    - `app/src/main/res/values/strings.xml`: `InfoPopupText` line 2 (version string and build date)
    - `app/build.gradle`: `android.defaultConfig`: `versionCode` and `versionName`
- [ ] When applicable, increment the target SDK level / Android version.
    - `app/build.gradle`: `android.defaultConfig.targetSdk`
    - `doc/Building_Tangram.texinfo`: “Testing” → “Using Virtual Devices”: move the new target version (and any others after the old target) from the list “suggested to ensure compatibility with new devices” to the preceding list “_highly recommended_ to test against the following API’s”, and move “which is the current target version” to the new target version.
- [ ] When applicable, increment the minimum SDK level / Android version.
    - `app/build.gradle`: `android.defaultConfig.minSdk`
    - `doc/Building_Tangram.texinfo`: “Testing” → “Using Virtual Devices”: remove all versions earlier than the new minimum from the list “_highly recommended_ to test against the following API’s”
- [ ] Rebuild the documentation
    1. `cd doc`
    2. `make`
    3. `cd ..`
- [ ] Write up a change log entry for all outstanding changes
- [ ] Write up a brief user-facing change log for F-Droid
    - `fastlane/metadata/android/en-US/changelogs/`_versionCode_`.txt`
- [ ] `git add` all modified files and `git commit`
- [ ] Build and sign the APK
    1. `./gradlew assembleRelease`
    2. Check `build.gradle`s `android.buildToolsVersion` for the tools version; set `BUILD_TOOLS_VERSION_DIRECTORY` to `~/src/Android_SDKs/build-tools/` plus the tools version.
    3. Set `ANDROID_KEYSTORE` to the keystory location (`../.private/*.keystore`)
    4. Set `SIGNING_KEY_ALIAS` to the name of your signing key (e.g. “MyKey”)
    5.
    ```
    ${BUILD_TOOLS_VERSION_DIRECTORY}/apksigner sign \
      --alignment-preserved \
      --ks ${ANDROID_KEYSTORE} --ks-key-alias ${SIGNING_KEY_ALIAS} \
      --out app/Tangram-${VERSION}.apk \
      app/build/outputs/apk/release/app-release-unsigned.apk
    ```
- [ ] Tag the commit with the version name
    - `git tag v`_versionName_
- [ ] Push the commit tag up to GitHub
    - `git push github master v`_versionName_
- [ ] Create the new release
    1. Open the [Releases](https://github.com/Typraeurion/Android-Tangram/releases) page in GitHub.  Open [Draft a new release](https://github.com/Typraeurion/Android-Tangram/releases/new) in a new window; use the previous release as a template to fill in the next one.
    - Select the tag for this release.
    - Title the release (e.g. “_Android Version_ Update”, “_Bug_ Patch”).
    - Enter release notes; generally the same items listed in the F-Droid change log.
    - Upload the APK from `app/Tangram-`_versionName_`.apk`
    7. “Publish release”
- [ ] Within the next couple of days, check the [F-Droid build monitor](https://monitor.f-droid.org/builds/build) to see whether the build was successful or failed.
