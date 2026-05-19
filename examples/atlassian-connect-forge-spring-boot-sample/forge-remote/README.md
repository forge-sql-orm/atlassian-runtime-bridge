# Forge-only manifest (`sample-forge-remote`)

This module is an **extension of [`forge-connect`](../forge-connect/)**: same Spring Boot app, same `core` / `frontend` artifacts, same Custom UI bundle (`../forge-connect/customUI`). It exists so you can **`forge deploy`** a manifest that reflects a **completed Connect → Forge migration**.

## What changed vs hybrid `forge-connect/manifest.yml`

- **`connectModules` removed** — no Connect lifecycle URLs, no `jira:generalPages` iframe entry.
- **Remote key** `forge-remote` instead of `connect` (`remotes`, `app.connect.remote`, endpoints).
- **Forge UI endpoint** `forge-remote-ui` (resolver on the global page) instead of `forge-remote` keyed to the old remote name.
- **Custom UI path** `../forge-connect/customUI` (built by the `frontend` module into the hybrid module tree).

`app.connect` remains in the manifest so Atlassian can link the Forge app to your existing Connect app key during migration tooling; there is no Connect JWT authentication block and no Connect modules to install in Jira.

## How to run

1. Build the reactor from the [sample parent](../): `mvn clean install`
2. Start Spring from **`forge-connect`**: `cd ../forge-connect && mvn spring-boot:run`
3. Configure **`remotes[].baseUrl`** in [`manifest.yml`](manifest.yml) and **`addon.base-url`** in [`forge-connect/src/main/resources/application.yml`](../forge-connect/src/main/resources/application.yml) to that URL.
4. Deploy from **this directory**:

```bash
cd examples/atlassian-connect-forge-spring-boot-sample/forge-remote
forge deploy
forge install
```

See the [sample README](../README.md) for prerequisites, `app.id` sync, and verification steps.
