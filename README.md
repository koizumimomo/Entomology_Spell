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

**Items**

- Swarm Spell Book: the Swarm school spell book
- Queen Bee Crown: your attacks charm the target (stacking vulnerability)
- Weaver Spider Chelicerae: insect creatures no longer attack you on sight
- Bee Incarnation: your summons may birth new summons on kill
- Swarm Staff / Swarm Rune / Swarm Upgrade Orb: casting staff and upgrade materials
- Royal Jelly / Spider Venom Gland / Insect Crystal: drops from swarm spell kills, used in crafting

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

| # | Feature | Effect |
|---|---|---|
| 1 | Swarm Exemption* | New effect + spell, cast on a specific target so your insects can attack it, bypassing friendly-fire protection (fixes stalemates vs. enemy summons) |
| 2 | Honey Courier* | Summons a non-attacking bee that returns to the caster after 8~2s (shorter at higher spell levels), restoring health, mana and some saturation, then vanishes |
| 3 | Parasitic Breath* | Silverfish-exclusive effect (like Chaotic Stinger): summoned silverfish inflict Hunger and stacking Slowness |
| 3.5 | Chaotic Stinger tweak | Poison/Wither become stackable |
| 4 | Swarm Call* | Grants the caster a buff of the same name: a chance to summon bees that strike back at attackers |
| 5 | Queen's Majesty* | All own summons within 32 blocks gain Resistance I, Regeneration I, Strength II, Speed I, Haste I and deal 20% more damage |

\* Working title, subject to change.

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
