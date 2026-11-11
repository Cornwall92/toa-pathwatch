# ToA Pathwatch

![ToA Pathwatch](icon.png)

Version 0.1.0 — development build.

A movable RuneLite panel showing the current ToA boss's selected invocations,
active path-levelling invocations, and a static explanation of that path level's effects.
Hold **Alt** and drag the panel below the boss health display. This version does
not automatically attach itself to the vanilla health bar or another plugin's overlay.

## Use

1. Use JDK 17 (the plugin compiles for Java 11), extract this project and open PowerShell in its folder.
2. Build: `.\gradlew.bat clean build`
3. Launch the development client: `.\gradlew.bat run`
4. Enable **ToA Pathwatch** in RuneLite.
5. In the raid lobby, open **your current party's Invocations tab** before entering
   each raid. After loading a preset, keep that tab open until its selections and
   raid level have finished updating. The plugin reads the resulting full selection.
6. Enter a boss room, then Alt-drag the reference panel into position. Width and
   optional general invocations/puzzle-room display are configurable.

For Jagex accounts, follow the official login guide:
https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
Keep any exported credentials on your own computer.

This source project is not available in the Plugin Hub. Copying a JAR into your
ordinary RuneLite folders does not install it as a Hub plugin.

## Behaviour and limits

- Contains all 44 invocation names and their UI positions/raid points.
- By default lists current-boss and path-levelling invocations. Other raid-wide
  selections can be enabled in configuration.
- Reads selections from the visible party interface and freezes a fresh capture
  for that raid. A complete zero-invocation selection is supported.
- Preset loads replace the whole observed selection, including invocations that
  were switched off. Preset names/files are not needed. Reopen the party tab after
  changes made while it was closed.
- Missing/partial data produces **Not captured**, rather than a misleading empty list.
- Enabling the plugin after entering a raid, logging out, or hopping clears the
  invocation capture. Path effects can still be shown from the current path HUD.
- Reads path level separately. HP/damage bonuses are path-only multipliers, not
  final damage predictions or total raid/team scaling.
- Levels 2 and 4 change mechanic tiers. Levels 5 and 6 only increase the displayed
  path scaling; they do not add another mechanic tier.
- No attack countdowns, live mechanic counters, prayer instructions, or tile indicators.
- Wardens show their selected invocations and have no invented independent path level.
  Phantom-specific inherited effects are outside this version's scope.

## Verification

The clean Gradle build passed on 11 November 2026: **48 tests, zero failures,
errors or skips**, with no compiler or Gradle warnings. Verified against RuneLite
1.13.6 using Gradle 8.10 and Temurin JDK 17.0.21, with Java `--release 11`.

Automated checks cover the invocation catalogue and path scaling, complete/partial
widget parsing, leader and non-leader reads, preset replacement, raid entry,
room-loading gaps, exit/reset, and late raid-level updates. The GitHub Actions
workflow runs the Gradle build and tests for pushes and pull requests.

No live RuneScape session was used. The interface formats, non-leader fallback,
room transitions and overlay placement still need the in-game checks below.
These checks do not establish Plugin Hub acceptance.

## In-game acceptance checklist

- Solo leader: open the invocation tab, enter each boss, compare the listed names.
- Load preset A, then B (including two with equal scores), open the current party
  invocation tab and enter. Confirm only B is shown. Repeat after manual edits.
- Non-leader: join a party, open its invocation tab and repeat. The label-color
  fallback and score widget formats particularly need live confirmation.
- Zero raid level: verify **None relevant enabled**, rather than a missing capture.
- Walk the Path: compare the panel's path level with the game's HUD after room changes.
- Check path levels 0/1, 2/3 and 4/5/6, especially Akkha's 4/5/6 sequence lengths.
- Change selections between consecutive raids and confirm no previous raid's list remains.
- Disable/re-enable, logout/reconnect, hop worlds, and enable mid-raid: no stale selections.
- Try fixed/resizable/stretched mode and Alt-drag below the health bar.

## Publication

RuneLite currently lists new high-end PvM boss plugins among features not being
considered. This reference-only presentation still requires an eligibility decision
before any Hub submission. No acceptance or Jagex approval is claimed.

See **RESEARCH.md** for the full name list, exact API mappings, sources, and uncertainties.
