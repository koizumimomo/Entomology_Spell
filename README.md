# [ES] Entomology Spell 虫群与我

**English** | [简体中文](./README_zh-CN.md)

An addon mod for **Iron's Spells 'n Spellbooks** that adds a brand-new **Swarm (Insect)** school of magic to Minecraft 1.20.1 (Forge). Command bees, spiders and cockroaches to fight for you.

## Download

- Latest: [Releases](../../releases)
- CurseForge: [es-entomology-spell](https://www.curseforge.com/minecraft/mc-mods/es-entomology-spell)
- Modrinth: [esentomology_spell](https://modrinth.com/mod/esentomology_spell)
- Other links: [Bilibili](https://space.bilibili.com/26216524)

## Features

**Spells**

| Spell | Description |
|---|---|
| Insect Pheromone | Marks a creature (or yourself) so it is treated as an arthropod |
| Swarm Aegis | A protective swarm reduces incoming damage; fireflies strike your targets when you attack |
| Summon Bee Swarm | Bees attack the last creature you struck; your attacks stack Swarm Wrath on targets |
| Chaotic Stinger | Your summoned bees inflict Poison/Wither on every sting |
| Parasite | A parasite incubates inside the target; when it bursts, silverfish hatch and nearby insects gang up on it |
| Web Entangle | Roots the target in cobwebs; the web can be destroyed to free them |
| Bee Stinger | Fires a swift stinger projectile |
| Bee Requiem | Three kamikaze bees dive through everything onto the target |
| Bee Alarm | Stationary turret bees auto-fire at hostiles in range |
| Summon Shiraori | Summons Shiraori, the Spider Mother: raises two spider nests on arrival, lays insect eggs that hatch into bugs, webs enemies, and summons a guardian every 30s (5 min, recast to dismiss) |
| Summon Frost Spider | A rideable frost spider battle pet |
| Summon Cockroach Dance Troupe | Maraca-shaking cockroaches grant a rhythm buff aura |
| Summon Mosquito Swarm | Crimson mosquitoes dive-bomb prey (players first, then hostiles); Blood Symbiosis heals your nearby summons on every hit |
| Swarm Exemption (Attack Order) | Marks a target with Swarm: Attack — all your summons (bees, butterflies, mosquitoes, etc.) will attack it, bypassing insect kinship and friendly-fire protection |
| Honey Courier | Summons a non-attacking bee that returns after a few seconds, restoring health, mana and saturation, then vanishes |
| Parasitic Breath | Summoned silverfish inflict Hunger and stacking Slowness on every bite |
| Swarm Call | Grants a buff: when hit, a chance to summon counterattack bees that strike the attacker |
| Butterfly Lovers (化茧成蝶) | Transforms vanilla spiders (incl. cave spiders) into friendly Ice Spiders; with Alex's Mobs: Fly → Crimson Mosquito → Warped Mosco (one step per cast) |
| Summon Butterfly | Summons butterflies that lift enemies into the air |
| Summon Butterfly Princess | Summons a butterfly princess who casts swarm spells (Summon Swarm, Confusion Sting, Small Bee Sting, etc.) |

**Items**

- Swarm Spell Book: the Swarm school spell book
- Queen Bee Crown: your attacks charm the target (stacking vulnerability)
- True Queen Crown: upgraded crown (Queen Bee Crown + Butterfly Spirit); summons butterflies, grants Queen's Majesty buff to wearer and buffs all summons
- Butterfly Spirit: obtained from butterfly princess trades; crafting ingredient for True Queen Crown
- Butterfly Wings (Blue / White): Curios back-slot accessory; grants creative flight; obtained from butterfly princess trades (4 insect crystals + 2 butterfly spirits)
- Weaver Spider Chelicerae: insect creatures no longer attack you on sight
- Bee Incarnation: your summons may birth new summons on kill
- Swarm Staff / Swarm Rune / Swarm Upgrade Orb: casting staff and upgrade materials
- Royal Jelly / Spider Venom Gland / Insect Crystal: drops from swarm Spell kills, used in crafting
- Shiraori's Fang: crafted (4 cobwebs + 2 spider venom glands + 2 spider eyes + 1 insect crystal; not consumed). Usable on a spider **only on a full-moon night at night** to begin a 30-second ritual — the spider is frozen inside a spinning ley-line circle while non-insect monsters are drawn to attack it. If it survives it becomes a permanent "Shiraori's Attendant" that always drops a Summon Shiraori scroll on death; if it dies during the ritual nothing drops
- Spawn eggs: Butterfly / Butterfly Princess / Cicada / Bug Beetle / Shiraori / Guardian Spider

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.10+ |
| Iron's Spells 'n Spellbooks | 1.20.1-3.16.3 |
| GeckoLib | 1.20.1-4.8.4 |
| Curios | 5.14.1+ |
| Alex's Mobs (optional) | 1.22.9 |

## Install

1. Install Forge 47.4.10+;
2. Drop the dependencies and this mod's jar into your `mods` folder;
3. Launch the game and look for the **Swarm** school in your spell book.

## Planned Updates (Todo)

All currently planned features have been implemented. No new goals at this time.

## Changelog (v1.4.x)

### New Spells

- **Attack Order** (swarm_exemption): Marks the target with Swarm: Attack — all your summons (bees, butterflies, mosquitoes, etc.) will attack it, bypassing insect kinship and friendly-fire protection
- **Honey Courier** (honey_courier): Summons a peaceful bee that returns after a few seconds, restoring health, mana and saturation, then vanishes
- **Parasitic Breath** (parasitic_breath): Your summoned silverfish inflict stackable Hunger and Slowness on every bite
- **Swarm Call** (swarm_call): Grants you the Swarm Call state — when hit, a chance to summon bees that strike back at the attacker
- **Summon Butterfly** (summon_butterfly): Summons a swarm of butterflies that lift the target into the air and drop them for fall damage. If a butterfly dies, there is a chance to spawn a new one
- **Butterfly Lovers** (butterfly_lovers): Transforms certain insects into stronger forms (Spider/Cave Spider → Frost Spider; with Alex's Mobs: Fly → Crimson Mosquito → Warped Mosco)
- **Summon Butterfly Princess** (summon_butterfly_princess): Summons a Butterfly Princess who fights for you for 5 minutes, casting insect spells at your enemies

### Tweaks

- Summon Cockroach Dance Troupe (summon_cockroach_dance) now has a chance to summon a Rainbow Cockroach
- Summoned Crimson Mosquitoes (summon_mosquito_swarm) now inflict Weakness I on their targets
- Poison and Wither dealt by bees under the Chaotic Stinger (chaotic_stinger) effect are now stackable
- Summons from Summon Bee Swarm (summon_bee_swarm), Spider Nest (spider_nest) and Summon Mosquito Swarm (summon_mosquito_swarm) now gain bonus health scaling with spell level and spell power — no longer fragile
- Added two-cast (recast) support to Summon Bee Swarm (summon_bee_swarm), Spider Nest (spider_nest), Summon Mosquito Swarm (summon_mosquito_swarm), Bee Alarm (bee_alarm), Summon Frost Spider (summon_ice_spider), Summon Cockroach Dance Troupe (summon_cockroach_dance) and Summon Butterfly Princess (summon_butterfly_princess) — second cast dismisses the summons
- Silverfish now automatically seek and attack nearby hostile mobs

### New Features

- Butterflies now spawn naturally around flowers in the world. Right-click a butterfly with Royal Jelly (royal_jelly) to obtain a Summon Butterfly (summon_butterfly) scroll. Right-click with an Insect Crystal (insect_crystal) to turn it into a Butterfly Princess (NPC)
- **NPC Butterfly Princess**: Can trade. Use 6 Insect Crystals (insect_crystal) + 4 Royal Jelly (royal_jelly) to trade for a Summon Butterfly Princess (summon_butterfly_princess) scroll, plus the crown upgrade material — Butterfly Spirit (butterfly_spirit). If attacked, she will fight back with swarm spells just like a summoned one. (Drops: ink sacs, swarm spell materials, random flowers)
- **Swarm: True Queen Crown** (true_queen_crown): Crafted from Swarm: Queen Bee Crown (queen_bee_crown) + Butterfly Spirit (butterfly_spirit). Permanently grants the Queen Bee (queen_bee) effect — summons within 32 blocks gain Resistance I, Regeneration I, Strength II, Speed I, Haste I and deal 20% more damage

### v1.4.9 Fixes

- The Curios **back** slot for the Butterfly Wings is now guaranteed: the mod ships its own back-slot definition and ensures every player has one on login/respawn, so the wings work even in a minimal mod setup
- Butterfly Wings (blue/white) now have a max stack size of 1 instead of 64
- Fixed the white Butterfly Wings model using incorrect UV mapping

## Changelog (v1.5.x / v1.6.0)

### New Spell

- **Summon Shiraori** (summon_shiraori): summons Shiraori, the Spider Mother — a boss-tier stationary nest-keeper (200 HP, 4 armor, 3.0x4.0 hitbox, 5 min duration, recast to dismiss). 30 ticks after spawning she raises 2 spider nests nearby (each immediately hatches its first wave of spiders); every 5-8s she lays an insect egg at her feet / within 3 blocks (normal or purple variant, max 4 within 6 blocks); every 30s she summons a Guardian Spider or casts Summon Frost Spider (50/50 in and out of combat); every 4-6s she casts Web Entangle (1s root, no damage) on enemies within 10 blocks and line of sight. When she, a nest or an egg is hurt, every same-owner bug within 16 blocks swarms the attacker. Not craftable on the scroll table — only obtainable via the Shiraori's Fang ritual

### Tweaks

- **Removed the Spider Nest summon spell**: the Spider Nest entity is now spawned only by Shiraori (owner = Shiraori; it collapses when she dies/despawns). Nests also actively guard Shiraori (defendAgainst + target sync every 40 ticks)
- **Insect egg hatch pool reworked**: 5% Summoned Warped Mosco; the remaining 95% is split evenly (~15.83% each) among Summoned Cicada / Summoned Bug Beetle / Summoned Silverfish / 3-segment Summoned Centipede (not rideable) / Summoned Cockroach / Summoned Crimson Mosquito. Without Alex's Mobs the four Alex-dependent slots are dropped and the remaining 3 share 100%
- Every egg hatchling is now a custom "summoned xxx" IMagicSummon entity (no reused wild EntityType; shouldBeSaved=false prevents no-AI chunk-reload shells)
- **Ally checks fully UUID-based**: all summoned bugs share `SwarmCreatures.isSameOwnerChain()` (up to 5 hops of SummonManager ownership). WebEntangleSpell's raycast now takes an ally predicate, so Shiraori can no longer web her own egg hatchlings or same-chain summons
- **Balance**: Shiraori hitbox 3.0x4.0, Guardian Spider 3.2x2.8 (stops bee_incarnation offspring stacking); Shiraori no longer follows/teleports — stationary nest-mother by design; guardians anchor within 16 blocks (teleport at 32) and fall back to following the player / independent hunting after she despawns; bee_incarnation offspring lifetime 10 min → 30s

### New Content

- **Cicada** (cicada): spawns naturally on trees in plains/forest biomes, two colour variants (30% alt texture). Every 20 ticks applies Slowness III + Darkness for 3s to non-allies within 6 blocks. Passive
- **Bug Beetle** (bug_beetle): spawns naturally underground below Y=50 (5 attack, 2 armor). Neutral, wanders like cockroaches, attacks non-creative players without Swarm Exemption and hostile mobs; never hits same-owner summons
- **Insect Egg block** (insect_egg): laid by Shiraori, hardness 0.3, survival-breakable (no hatch if broken). BlockEntity NBT persists the hatch timer (1200-2400 ticks) and ownerUUID. Normal/purple variants
- **Guardian Spider** (guardian_spider): summoned by Shiraori, leap attack; goes berserk for 30s (2.5x leap distance + area knockback) when a nest/egg is broken or Shiraori is hurt; isAlly UUID + setTarget() hard-gate
- **Shiraori's Fang** (shiraori_s_fang): see Items above — **v1.6.0** restricts the ritual to full-moon nights and adds the spinning two-layer ley-line circle under the ritual spider
- Spawn eggs for Shiraori / Guardian Spider / Bug Beetle / Cicada (added to both the mod creative tab and the vanilla Spawn Eggs tab)

### Fixes

- Fixed purple-black missing textures on new spawn eggs (missing item models) and on the insect egg block (three-stage investigation: leftover Blockbench hitbox cube → block models must reference textures/block/ not textures/entity/ → alpha textures need the cutout render layer in-world)
- Added the missing `ui.irons_spellbooks.summon_duration` lang key in all three languages (also fixes the raw key shown on the Summon Butterfly Princess scroll)
- Fixed Guardian Spiders turning into idle statues after Shiraori despawned (owner==null rejected every target): dual-owner model — Shiraori → summoning player → independent roaming/hunting

## Implemented Features

| Feature | Effect |
|---|---|
| Swarm Exemption (进攻指令) | Cast on a target to mark it with Swarm: Attack — all your summons (bees, butterflies, mosquitoes, etc.) will attack it, bypassing insect kinship and friendly-fire protection |
| Honey Courier (蜜影迷踪) | Summons a non-attacking bee that returns after a few seconds, restoring health, mana and saturation, then vanishes |
| Parasitic Breath (寄生气息) | Summoned silverfish inflict Hunger and stacking Slowness on every bite |
| Swarm Call (蜂群呼唤) | Grants a buff: when hit, a chance to summon counterattack bees that strike the attacker |
| Queen's Majesty (女皇威严) | Transferred to True Queen Crown: wearer gains the buff, all summons within 32 blocks gain Resistance I, Regeneration I, Strength II, Speed I, Haste I and deal 20% more damage |
| Butterfly Wings (蝴蝶翅膀) | Curios back-slot accessory (blue/white) that grants creative flight; obtainable from butterfly princess trades |

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

Reason: this mod's code directly derives from and reuses entity classes from [Alex's Mobs](https://github.com/AlexModGuy/AlexsMobs) (GPL-3.0-only) — `EntityCockroach`/`RenderCockroach`, and since v1.3.0 `EntityCrimsonMosquito`/`RenderCrimsonMosquito`. Under GPL's copyleft terms, derivative works must be released under GPL-3.0.

See the [LICENSE](./LICENSE) file for the full license text.

### Dependency Licenses

| Dependency | License |
|---|---|
| Alex's Mobs | GPL-3.0-only |
| Iron's Spells 'n Spellbooks | All Rights Reserved (addons allowed as a dependency) |
| GeckoLib | MIT |
| Curios | MIT |

## Credits

The butterfly entity models, butterfly princess models, and butterfly-related spell concepts in this mod were inspired by and adapted from designs originally created by:

- **alphagrievous** — butterfly & butterfly princess model concepts
- **Sleepy reii reii** — butterfly & butterfly princess model concepts

We are grateful for their creative work and inspiration.
