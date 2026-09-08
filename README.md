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
| Spider Nest | Periodically spawns spiders to defend the area; wild spiders nearby join the defense |
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
- Butterfly Spawn Egg / Butterfly Princess Spawn Egg: creative spawn eggs

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
