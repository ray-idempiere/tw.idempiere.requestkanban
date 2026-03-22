# Contributing to tw.idempiere.requestkanban

## Prerequisites

- Java 17 JDK
- Maven 3.8+
- iDempiere 12 source checked out as a sibling directory (`../idempiere/`)

## Build

```bash
mvn package -Drevision=12.0.0
```

The output JAR is in `target/`. The parent POM is resolved from the sibling `../idempiere/org.idempiere.parent/` directory.

## Deploy

1. Copy the built JAR into your iDempiere installation's `plugins/` directory.
2. Either restart iDempiere, or run `update` from the OSGi console.
3. The "Request Kanban" dashboard item will appear in iDempiere's dashboard configuration.

## Configuration (Optional)

Two `AD_SysConfig` keys control status visibility. Set them under
**System → Client Info → System Configurator**:

| Key | Default | Description |
|-----|---------|-------------|
| `RK_HiddenStatuses` | _(empty)_ | Comma-separated `R_Status.value` entries to hide from the Kanban/List views. Values are case-sensitive and must match `R_Status.value` exactly. Example: `Final,Cancelled` |
| `RK_ActiveStatuses` | `Processing,Open` | Comma-separated `R_Status.value` entries that count as "active" for the high-priority request counter. |

## Running Tests

```bash
mvn test -Drevision=12.0.0
```

Tests cover `StatusConfig` (pure Java, no iDempiere container required).

## Reporting Bugs

Open a GitHub Issue with:
- iDempiere version
- Java version (`java -version`)
- Steps to reproduce
- Relevant server log output

## Pull Requests

- One feature or fix per PR.
- Commit messages in English, imperative form (e.g., "Fix null pointer in drag handler").
- Follow existing code style. Do not add external dependencies without discussion in an issue first.
