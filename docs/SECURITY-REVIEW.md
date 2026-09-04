# Lodgy security review (LODGY-46)

Reviewed 2026-09-04 against the app as of this commit. Scope: the OWASP Mobile Top 10 surfaces
that actually exist in a local-only, no-backend Android app — exported components, file sharing,
credential storage, and untrusted file import. There is no network permission and no server, so
whole categories (insecure communication, server-side auth, API key handling) have no surface here.

Severity is judged against this app's real threat model: a single warden's phone, holding tenant
names, phone numbers, ID-proof photos and rent records. The attackers worth designing against are
another app on the same device, and someone who picks the phone up. A rooted/forensically-imaged
device is explicitly out of scope for MVP — DESIGN.md 5 defers encryption at rest.

## Findings

### 1. Exported activity accepted an unvalidated navigation route — Medium — FIXED

`MainActivity` is exported (it must be, to be launchable). It reads `EXTRA_NOTIFICATION_ROUTE`
from its intent and, after unlock, hands it to `navController.navigate()`. Any app on the device
can start Lodgy with an arbitrary value for that extra, and Navigation Compose throws on an
unknown route — so a third-party app could crash Lodgy on every launch until the user cleared it.

No data was reachable this way: the route is replayed only after the PIN gate, so it cannot be
used to skip authentication or read anything.

Fixed in `notify/NotificationRoutes.kt`: `isSupportedNotificationRoute()` allow-lists the three
routes notifications actually build, and `LodgyNavHost` follows the extra only if it passes.
Covered by `NotificationRoutesTest`.

### 2. FileProvider exposed the whole cache directory — Low — FIXED

`file_paths.xml` declared `<cache-path path="."/>`, making every file under `cacheDir` eligible to
be shared through the provider. That directory also holds `restore_staging` during an import,
which contains a full unencrypted copy of the database.

Exploiting it would have required the app itself to mint a URI for one of those files, and it only
ever does so for a camera output file it names — grants are per-URI, not per-directory, so no app
could enumerate the cache. Still broader than anything needed.

Fixed: the provider now shares only `cacheDir/camera/`, and `PhotoStorage.createCameraOutputUri()`
writes there. The restore staging area is no longer inside any shareable path.

### 3. No backoff or lockout on repeated PIN attempts — Medium — OPEN, recommend a ticket

`PinLockViewModel` verifies and clears the field on failure, with no delay, no attempt counter and
no lockout. Someone holding the unlocked-but-locked phone can try PINs as fast as they can tap.
A 4-digit PIN is 10,000 possibilities; that is not hand-guessable in one sitting, but it is not
much protection either, and there is nothing to slow a scripted attack over ADB.

Suggested fix: count consecutive failures in `AuthPreferences` and impose an increasing delay
(e.g. free for the first 5, then 30s, then a minute, persisted so a process restart does not reset
it). Left as a product decision rather than changed here — it adds a way for a warden to lock
themselves out of their own records, which deserves a deliberate choice.

### 4. Short PIN space versus an offline attacker — Low — ACCEPTED

`PinHasher` uses `BCrypt.gensalt()` (per-hash random salt, cost factor 10, salt embedded in the
hash) and `BCrypt.checkpw` for verification. That is correct usage; there is no fixed salt, no
home-rolled hashing and no fast digest anywhere.

The residual weakness is the input space, not the algorithm: 4–6 digits is 10^4–10^6 candidates,
which bcrypt at cost 10 slows but does not stop for anyone who has already extracted `lodgy.db`.
Extracting it requires root or a device image, which DESIGN.md 5 places out of scope for MVP.

Accepted, with two mitigations already in the plan: LODGY-50 lets a warden choose 6 digits, and
the roadmap's SQLCipher item removes the offline-attack surface entirely.

### 5. Zip-slip in backup import — re-verified, no finding

Re-checked rather than assumed, per the ticket. `BackupManager.stageImport` handles exactly two
entry shapes:

- the database, matched by exact string equality with `DB_ENTRY_NAME`, so no traversal is possible;
- photos, where the name after the `photos/` prefix is written only if it is non-blank, contains
  no `..`, and contains no `/`.

That rejects `../../x`, `foo/../x`, and any nested path — the file always lands directly in the
staging photos directory. Every other entry is ignored entirely. Backslash is not a path separator
on Android, so `a\b` becomes a literal filename rather than an escape. `applyStaged` then copies
by `File.name` out of a directory the app created itself. `BackupManagerTest` already covers the
rejection case.

### 6. No size or entry-count limit on an imported zip — Low — ACCEPTED

`stageImport` extracts every matching entry with no cap, so a crafted zip could fill the device's
cache before the warden ever confirms the restore. The staging directory is deleted on failure, so
the effect is transient.

The file is chosen by the warden through SAF from their own storage, which makes this a
self-inflicted denial of service rather than an attack path. Accepted for MVP; if it ever matters,
cap the total extracted bytes and bail out.

## Reviewed and clean

- **Manifest components.** `MainActivity` is the only exported component, as a launcher requires.
  The `FileProvider`, WorkManager's `InitializationProvider` (with its default initializer removed
  in favour of the app's own `Configuration.Provider`) and `AppLocalesMetadataHolderService` are
  all `exported="false"`. No receivers, no exported services, no custom permissions needed.
- **`allowBackup="false"`.** Correct and important here: it stops `adb backup` from lifting the
  unencrypted database and photo directory off the device.
- **Permissions.** Exactly one: `POST_NOTIFICATIONS`, runtime-requested and degrading gracefully
  when denied. No `INTERNET`, no `SEND_SMS`, no storage permissions — the app reaches other apps
  only through user-driven intents and SAF.
- **Logging.** No `Log`, `println` or `printStackTrace` anywhere in `app/src/main`, so no tenant
  data reaches logcat.
- **Outbound intents.** The reminder and quick-contact paths use `ACTION_DIAL`, `wa.me` and
  `smsto:` — all of which open another app pre-filled and require the warden to send. Nothing is
  dialled or transmitted automatically.
