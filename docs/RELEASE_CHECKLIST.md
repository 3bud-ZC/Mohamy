# MohamyPhone Release Checklist

Current warning:
The app is currently set to `versionName = "1.8.0"` and `versionCode = 12` in `app/build.gradle.kts`, but `update/latest.json` still points to `1.7.1` / `11` because `v1.8.0` is not published yet. Do not point `latest.json` at `1.8.0` until the release page and APK asset both exist.

1. Confirm `app/build.gradle.kts` has the intended `versionCode` and `versionName`.
   - Confirm the new `versionCode` is higher than the version installed on the test device.
2. Build the release APK.
3. Confirm the APK is signed with the correct keystore for upgrade compatibility.
   - Confirm the signing key matches the key used by the already installed APK.
4. Create the GitHub tag for the release, for example `v1.8.0`.
5. Wait for the GitHub Actions release workflow to complete.
6. Verify the GitHub release page exists for the new tag.
   - Example pattern: `https://github.com/3bud-ZC/Mohamy/releases/tag/v1.8.0`
7. Verify the `app-release.apk` asset exists and returns HTTP `200` or `302`.
   - Verify the exact asset URL, not only the release page.
8. Only after that, update `update/latest.json`, or confirm that the workflow already updated it correctly.
9. Test the in-app update flow from a real installed previous version.
10. Never point `update/latest.json` to a non-existing APK URL.

Verification notes:
- Prefer the existing `.github/workflows/publish-release.yml` automation to update the release asset and `update/latest.json`.
- Treat `scripts/publish-release.ps1` as a tag/version helper, not proof that the public release asset is already available.
- If the workflow finishes but the asset URL is still missing or failing, leave `update/latest.json` unchanged until the asset is reachable.
