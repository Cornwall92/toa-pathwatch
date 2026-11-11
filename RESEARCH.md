# ToA invocation and path research

Checked 11 November 2026. Research supports the included development draft; it is
not a report of in-game verification.

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

Use `client.getWidget(InterfaceID.ToaPartydetails.INVOCATIONS_CONTENT)` — currently
interface **774**, child **52**. Read on the client thread after the party scripts
have built the children. Existing implementations use `ScriptPostFired` IDs **6729**
(invocation rebuild) and **6732** (reward-panel toggle); **6617** is also used for
the party build. A visible-grid read on `GameTick` catches delayed population.
Reads are coalesced through `ClientThread.invokeLater` so script-driven preset
loads can finish rebuilding the interface before the full selection is read.

For each label, identify its invocation and inspect the preceding background's
`getOnOpListener()`: when present, an integer at index **3** of **1** means selected;
**0** means unselected. This is an implementation detail of the game interface,
not a stable API contract.

**Non-leaders:** those click handlers may be absent. The Invocation Display author
reports selected labels with color **0xD39034**. The draft uses that fallback only
when all 44 labels are present and their selected point total matches the visible
party score. Its exact color and score text format still require live testing.
An unreadable/partial frame must remain unknown. A fully parsed list with a matching
score of zero is a legitimate empty selection.

The display plugin also tries interface 774:92 as an active-only list, but its
source explicitly describes that route as a hypothesis. It is not used here.
Nor does this draft use the old `spriteId < 4600` heuristic: current RuneLite
gamevals place `ToaInvocationIcons` at 8722–8745, so that shortcut is unsafe.

Cache a fresh capture before raid entry. Clear it on party/session changes and
raid exit. Freeze it for the raid rather than replacing it when lobby widgets
unload. The draft asks for the current party's Invocations tab before each raid;
mid-raid enabling does not pretend to reconstruct missing selections. Entry checks
the captured points against the raid-level varbit, retrying for up to five tick
observations while in the nexus if the score arrives late. Three consecutive
unknown-region ticks are tolerated during room transitions; a confirmed exit
clears immediately. Equal scores cannot prove equal selections, so reopening the
current party's invocation tab remains essential after any preset/party changes.

Sources:

- [Established preset reader](https://github.com/LlemonDuck/tombs-of-amascut/blob/cec1665058a3244bf41daddc0e1f43177a0b3edf/src/main/java/com/duckblade/osrs/toa/features/invocationpresets/InvocationPresetsManager.java)
- [Non-leader capture implementation and caveats](https://github.com/noahnoah711/toa-invocation-display/blob/2351fc4947872521aa226f10ef07e90577475a19/src/main/java/com/github/noahnoah711/toainvocationdisplay/ToaInvocationDisplayPlugin.java)
- [RuneLite interface gamevals](https://github.com/runelite/runelite/blob/bb0fa42b3d9329ffe31115f1f20042c455def2e3/runelite-api/src/main/java/net/runelite/api/gameval/InterfaceID.java)
- [RuneLite sprite gamevals](https://github.com/runelite/runelite/blob/bb0fa42b3d9329ffe31115f1f20042c455def2e3/runelite-api/src/main/java/net/runelite/api/gameval/SpriteID.java)

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

## Eligibility and remaining validation

The panel is designed as static reference text for a detected room/path level.
It does not respond to individual attacks or predict the next mechanic. This is
a design distinction, **not proof of approval**: RuneLite's current rejected-feature
list includes new high-end PvM boss plugins, and Jagex restricts additional combat
indicators. Obtain an eligibility decision before a Hub submission.

- [RuneLite feature restrictions](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)

The automated checks cover the catalogue, partial/zero selections, point consistency,
mechanic tiers, stat scaling and room filtering. They do not validate actual server
packets, live widget lifetimes, non-leader rendering, or overlay placement.
