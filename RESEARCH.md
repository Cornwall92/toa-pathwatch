# ToA invocation and path research

Invocation capture rechecked 13 November 2026 against RuneLite's revision-242
game-script dump. Other reference data was checked 11 November 2026. This is
source research, not a report of successful in-game verification.

## Invocation catalogue

44 invocations. Index is the zero-based position in the full party-interface list.
The traditional background/icon/label child positions are `3*i`, `3*i+1`, `3*i+2`.
Match the label text as well, rather than trusting position alone.

| Group | Indexes | Names in UI order (raid points) |
|---|---|---|
| Attempts | 0–3 | Try Again (+5); Persistence (+10); Softcore Run (+15); Hardcore Run (+25) |
| Time | 4–7 | Walk for It (+10); Jog for It (+15); Run for It (+20); Sprint for It (+25) |
| Helpful Spirit | 8–10 | Need Some Help? (+15); Need Less Help? (+25); No Help Needed (+40) |
| Paths | 11–14 | Walk the Path (+50); Pathseeker (+15); Pathfinder (+40); Pathmaster (+50) |
| Prayer | 15–16 | Quiet Prayers (+20); Deadly Prayers (+20) |
| Restoration | 17–19 | On a Diet (+15); Dehydration (+30); Overly Draining (+15) |
| Kephri / Scabaras | 20–24 | Lively Larvae (+5); More Overlords (+15); Blowing Mud (+10); Medic! (+15); Aerial Assault (+10) |
| Zebak / Crondis | 25–28 | Not Just a Head (+15); Arterial Spray (+10); Blood Thinners (+5); Upset Stomach (+15) |
| Akkha / Het | 29–32 | Double Trouble (+20); Keep Back (+10); Stay Vigilant (+15); Feeling Special? (+20) |
| Ba-Ba / Apmeken | 33–37 | Mind the Gap! (+10); Gotta Have Faith (+10); Jungle Japes (+5); Shaking Things Up (+10); Boulderdash (+10) |
| Wardens | 38–43 | Ancient Haste (+10); Acceleration (+10); Penetration (+10); Overclocked (+10); Overclocked 2 (+10); Insanity (+50) |

Cross-checked against the existing plugin authors' source:

- [LlemonDuck invocation enum](https://github.com/LlemonDuck/tombs-of-amascut/blob/cec1665058a3244bf41daddc0e1f43177a0b3edf/src/main/java/com/duckblade/osrs/toa/util/Invocation.java)
- [ToA Invocation Display names](https://github.com/noahnoah711/toa-invocation-display/blob/2351fc4947872521aa226f10ef07e90577475a19/src/main/java/com/github/noahnoah711/toainvocationdisplay/Invocation.java)

The implementation keeps explicit indexes instead of deriving identifiers from
enum declaration order. All 44 entries are also supplied in `invocations.json`.

## Reading active invocations

There is no verified public `client.getActiveToaInvocations()` API, and the total
raid level cannot identify its constituent invocations. Different selections can
produce the same total. Saved personal presets are not proof of the current
party's configuration.

Version 0.1.3 captures the **input to party-build script 6729** using RuneLite's
`ScriptPreFired.getScriptEvent().getArguments()`, then accepts the parsed snapshot
on `ScriptPostFired` only while the party screen is visible in the lobby. Script
6729 uses these same inputs to build both the invocation grid and summary. It
rebuilds the full selection after individual edits and preset loads.

| Event argument | Meaning |
|---|---|
| 0 | Script ID 6729 |
| 1 | Party role: 1 = member, 2 = leader; other roles are rejected |
| 2 | Additional party metadata, not used to identify selections |
| 3 | Active invocation count |
| 4 | Raid level |
| 5 | Party tab: 0 members, 1 applicants, 2 invocations, 3 summary |
| 6–8 | Three invocation selection words |

Script 6755 reads cache struct parameter 1159 as the bit index. Standard invocation
bits are 1 through 44 in catalogue order. Words are split at **31 and 62**, not
32 and 64. The reader validates the count and summed points and rejects any
unsupported/reserved bits, including the event/league-only Blazing Tombs entries.
A zero-selection, zero-score party is valid. Input arrays must have the expected
script ID, length and integer types; malformed data stays unknown.

The party role is established by script 6730: 2 produces Disband, 1 Leave, 3
Withdraw, and the other roles Apply. Thus another party with the same total cannot
be mistaken for the user's party. The screen may show any of its party tabs;
the normal user instruction remains to open Invocations and verify the capture.
An interface already open when enabling the plugin must be closed and reopened.

### Comparison with established plugins

| Plugin | Approach | Findings |
|---|---|---|
| Tombs of Amascut | Grid 774:52, leader `getOnOpListener()[3]`, scripts 6729/6732 | Uses selection state rather than cosmetic colour. Its preset cleanup can recolour all grid children white. |
| ToA Invocations | Same leader callback plus WidgetLoaded; retains cache across rooms | Missing non-leader callbacks are skipped, so this is not a complete non-leader solution. |
| ToA Invocation Display | Deferred reads, names plus sprite/colour/callback heuristics; retains prior cache on unsuccessful reads | Addresses transient and non-leader UI, but contains unsafe assumptions that must not be copied. |

The direct script snapshot reads the same authoritative selection that supplies
those callbacks, before plugins can recolour the widgets. It removes the old
all-label/colour dependency and the repeated tick reads entirely.

Do **not** copy these alternatives from the comparison plugin:

- `spriteId < 4600`: current invocation sprite gamevals have moved.
- `0xD39034`: script 6750 actually uses `0xD39F34`.
- Interface 774:92 as an active-name list: it is an invocation count.
- Interface 776:5 as a party selection: scripts 6609/6610 build a generic reference
  catalogue with every entry's active sprite, without any party selection input.

Script 6751 also embeds selection state in desktop hover callbacks for both roles,
but RuneLite's public Widget API exposes setters without corresponding getters.
That does not provide a supported fallback and no reflection is used.

### Lifecycle findings and validation

The previous implementation could erase a valid lobby candidate on a controller
varp notification before the first raid GameTick. It also invalidated a capture
on any unreadable open grid, including transient click styling. These are code
failure paths, not proof of which occurred in the user's game.

The replacement checks controller identity only with settled lobby geography,
accepts complete snapshots immediately after the party-build script, and does not
re-read transient widgets. An individual edit invalidates the candidate until the
server supplies a complete updated selection. Logout, confirmed party changes and
raid exit still clear it. Raid entry still requires matching points and allows up
to five observations in the nexus for late score updates. Equal scores alone never
identify a selection. The lobby panel displays capture success before the raid.

Sources checked against current upstream/Hub source on 13 November 2026:

- [Current game scripts, revision 242](https://github.com/runelite/cs2-scripts/tree/562463b3477faa9f850e5c3615c087f5a32fedc3/scripts): 6729, 6730, 6749, 6750, 6751, 6754, 6755, 6609 and 6610.
- [Tombs of Amascut preset reader and visual cleanup](https://github.com/LlemonDuck/tombs-of-amascut/blob/831c39b9d60f40965d221fe5ac6b4d19d61c97f4/src/main/java/com/duckblade/osrs/toa/features/invocationpresets/InvocationPresetsManager.java)
- [ToA Invocations reader](https://github.com/mxO-dev/toa-invocations/blob/84fa5bcc586a75178b599f307184fb8619787404/src/main/java/com/toainvocations/ToaInvocationsPlugin.java)
- [ToA Invocation Display reader](https://github.com/noahnoah711/toa-invocation-display/blob/2351fc4947872521aa226f10ef07e90577475a19/src/main/java/com/github/noahnoah711/toainvocationdisplay/ToaInvocationDisplayPlugin.java)
- [RuneLite ScriptPreFired API](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/events/ScriptPreFired.java)
- [RuneLite Widget API](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/widgets/Widget.java)

## Reading path levels and identifying the room

Dedicated values exist in `net.runelite.api.gameval.VarbitID`:

| Boss | Path | Constant | ID | Boss template region |
|---|---|---|---:|---:|
| Zebak | Crondis | TOA_CLIENT_CRONDIS_LEVEL | 14376 | 15700 |
| Kephri | Scabaras | TOA_CLIENT_SCABARAS_LEVEL | 14377 | 14164 |
| Akkha | Het | TOA_CLIENT_HET_LEVEL | 14378 | 14676 |
| Ba-Ba | Apmeken | TOA_CLIENT_APMEKEN_LEVEL | 14379 | 15188 |

Read with `client.getVarbitValue(VarbitID.TOA_CLIENT_HET_LEVEL)`, for example.
`TOA_CLIENT_RAID_LEVEL` is **14380**, and is a different quantity.
`TOA_CLIENT_CURRENT_PATH` is **14381**; this draft does not guess its enum encoding.

The existing Tombs of Amascut plugin reads the actual path number from the HUD:

- `InterfaceID.ToaHud.PATHS_LEVEL`: current path, **481:45**.
- `NEXUS_SCABARAS_LEVEL`: **481:49**.
- `NEXUS_AKKHA_LEVEL`: **481:51**.
- `NEXUS_APMEKAN_LEVEL`: **481:53** (the constant uses this spelling).
- `NEXUS_CRONDIS_LEVEL`: **481:55**.

The plugin reads the displayed current-path number, accepting only 0–6. A hidden
or unreadable HUD produces an unknown level. Dedicated varbit IDs are
source-verified but are not used as a fallback until their raw encoding and live
synchronization have been compared with the HUD during a raid.

Map instanced coordinates back to template coordinates with
`WorldPoint.fromLocalInstance(...)` before checking the region. Guard missing
players/local points/world views during login/loading. Puzzle regions are Crondis
15698, Scabaras 14162, Het 14674 and Apmeken 15186; nexus is 14160; Wardens use 15184
and 15696. No NPC-name guessing or repeated whole-scene scans are needed.

Sources:

- [Official varbit definitions](https://github.com/runelite/runelite/blob/bb0fa42b3d9329ffe31115f1f20042c455def2e3/runelite-api/src/main/java/net/runelite/api/gameval/VarbitID.java)
- [Existing HUD path-level tracker](https://github.com/LlemonDuck/tombs-of-amascut/blob/cec1665058a3244bf41daddc0e1f43177a0b3edf/src/main/java/com/duckblade/osrs/toa/features/PathLevelTracker.java)
- [Existing room mappings](https://github.com/LlemonDuck/tombs-of-amascut/blob/cec1665058a3244bf41daddc0e1f43177a0b3edf/src/main/java/com/duckblade/osrs/toa/util/RaidRoom.java)

## Path invocations and scaling

| Invocation | Effect |
|---|---|
| Walk the Path | Randomly increases unfinished paths as rooms are completed: two paths after the first completion, one after the second, one after the third. Read actual levels instead of predicting the random recipients. |
| Pathseeker | Starts all four paths at level 1. |
| Pathfinder | Starts all four paths at level 2. |
| Pathmaster | Starts all four paths at level 3. |

Only one of Pathseeker/Pathfinder/Pathmaster can be selected; Walk the Path can
combine with it. Mechanical tiers change at **2** and **4**, capped at 4.
Path health/damage increases continue through level 6:

| Path level | 0 | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Additional HP/damage | 0% | 8% | 13% | 18% | 23% | 28% | 33% |

This is a path-only factor for most enemies; raid-level/team scaling and exceptions
still apply. Do not present it as a defence bonus or an exact final-hit calculation.

Source: [ToA invocation rules and change history](https://oldschool.runescape.wiki/w/Tombs_of_Amascut).

## Boss mechanics by tier

| Path / boss | Levels 0–1 | Levels 2–3 | Levels 4–6 |
|---|---|---|---|
| Het / Akkha | Four Memory Blast steps | Five steps | Six steps |
| Apmeken / Ba-Ba | Normal rockfall/blast areas; rolling boulder base HP 25 | Faster rockfall; wider sarcophagus blast area; boulder base HP 27 | Further rockfall/blast-area increase; boulder base HP 31 |
| Scabaras / Kephri | Normal fireball behaviour and healing swarms | Faster fireballs; triple healing swarms can appear | Further speed increase; triple swarms occur more often |
| Crondis / Zebak | Normal attacks; wave gap three tiles | Faster attacks including enrage; gap two tiles | Five-tick normal/two-tick enraged attacks; gap one tile |

Akkha's final-phase white orb damage also scales with raid/path level and is capped
at 25. It is not safe to turn that into a fixed per-tier damage number.

Ba-Ba's 25/27/31 values are **base** boulder HP before other applicable scaling.
Rockfall speed changes must not be confused with the separate Boulderdash invocation.

For Kephri, the sources describe faster auto-attacks/fireballs but do not supply a
well-grounded numeric cadence/flight-time table. The draft therefore does not invent
a `6 → 5 → 4 ticks` sequence. Larger fireball area from **Aerial Assault** is a
separate selected invocation, not automatically a path-level effect. Arcane Scarab
charge speed is no longer increased by path levelling.

For Zebak, path speed changes concern projectile launches, not changing the
projectile's shattering timing. The draft uses qualitative text for levels 2–3;
the five/two-tick figures at level 4+ are explicitly documented in the achievement
guide. Three/two/one-tile gaps follow the documented one-tile narrowing per tier.

The Wardens do not have a fifth independent path level. Display their six possible
invocations separately; this draft does not model inherited phantom behaviour.

Sources:

- [Akkha](https://oldschool.runescape.wiki/w/Akkha)
- [Akkhan't Do it — six-step level-4 sequence](https://oldschool.runescape.wiki/w/Akkhan%27t_Do_it)
- [Ba-Ba](https://oldschool.runescape.wiki/w/Ba-Ba)
- [Kephri](https://oldschool.runescape.wiki/w/Kephri)
- [Zebak](https://oldschool.runescape.wiki/w/Zebak)
- [Rockin' around the croc](https://oldschool.runescape.wiki/w/Rockin%27_around_the_croc)

## Invocation hover descriptions (0.1.4)

The catalogue's short effect descriptions paraphrase the native invocation structs'
description parameter 1262 in [game cache 2762](https://archive.openrs2.org/caches/runescape/2762).
Blowing Mud's two-player targeting and lack of additional solo effect are also
documented in the [raid strategy reference](https://oldschool.runescape.wiki/w/Tombs_of_Amascut/Strategies).
Lively Larvae uses qualitative wording because the native description and observed
egg counts do not give a consistent exact multiplier.

Hover detection uses each rendered LineComponent's local bounds plus the overlay's
canvas position. RuneLite's TooltipManager handles placement and frame cleanup;
descriptions wrap at 300 pixels. These are static reference descriptions, with no
attack tracking or live recommendations. Both new settings are enabled by default;
hiding the list affects presentation only and does not disable capture.

## Eligibility and remaining validation

The panel is designed as static reference text for a detected room/path level.
It does not respond to individual attacks or predict the next mechanic. This is
a design distinction: RuneLite's current rejected-feature list includes new
high-end PvM boss plugins, and Jagex restricts additional combat indicators.
The initial reference panel was accepted in
[Hub PR #20523](https://github.com/runelite/plugin-hub/pull/20523); future changes
remain subject to review.

- [RuneLite feature restrictions](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)

The automated checks cover the catalogue, partial/zero selections, point consistency,
mechanic tiers, stat scaling and room filtering. They do not validate actual server
packets, live widget lifetimes, non-leader rendering, or overlay placement.
