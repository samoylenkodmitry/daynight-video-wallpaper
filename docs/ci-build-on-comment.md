# Build APK on PR Comment

This workflow builds the Android application whenever a trusted maintainer comments `#buildapk` on a pull request. It uploads the generated APKs as artifacts and replies on the pull request with the download link.

## How it works

1. The [`build-on-comment.yml`](../.github/workflows/build-on-comment.yml) workflow listens for new PR comments.
2. It validates that the comment body contains `#buildapk` (case-insensitive) and that the commenter is an owner, member, or collaborator.
3. The workflow checks out the PR's merge ref and falls back to the head ref if the merge ref is unavailable.
4. Using Temurin JDK 21 and the Gradle cache, it runs the Gradle task specified by the `BUILD_TASK` environment variable (defaults to `assembleRelease`).
5. Any APKs found in `app/build/outputs/apk` are uploaded as the artifact `apk-<pr>-<run>` along with an `apklist.txt` manifest.
6. A success comment posts the artifact link, commit SHA, build duration, and APK count. Failures add a comment with a link to the workflow logs.
7. If an untrusted user triggers the command, the workflow exits early without building and logs the skip reason.

## Usage

* Add a comment containing `#buildapk` to any open pull request.
* Wait for the GitHub Actions run to complete; you'll receive a PR comment with the artifact link when it succeeds.
* Artifacts are retained for 7 days and are accessible to repository collaborators.

## Customizing the build

* Set the `BUILD_TASK` environment variable in the workflow to run a different Gradle task, such as `assembleDebug` or `:app:bundleRelease`.
* Adjust the artifact path if your APKs are generated in a different module or directory structure.

## Troubleshooting

* **No APKs found** – Verify that the Gradle task outputs to `app/build/outputs/apk`. Update the workflow paths if needed.
* **Build failures** – Check the linked workflow logs in the failure comment.
* **Skip due to trust** – Ensure the commenter is an owner, member, or collaborator of the repository.
