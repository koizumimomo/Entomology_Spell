# [ES] Entomology Spell 虫群与我

[English](./README.md) | **简体中文**

为 **Iron's Spells 'n Spellbooks（铁魔法与法术书）** 添加全新派系「虫群（Swarm / Insect）」的附属模组。操纵蜂群、蛛群与蟑螂大军，用虫子的方式解决战斗。

## 下载 Download

- 最新版本 Latest: [Releases](../../releases)
- CurseForge: [es-entomology-spell](https://www.curseforge.com/minecraft/mc-mods/es-entomology-spell)
- Modrinth: [esentomology_spell](https://modrinth.com/mod/esentomology_spell)
- 其他链接 Other links: [Bilibili](https://space.bilibili.com/26216524)

## 功能一览 Features

**法术 Spells**

| 法术 Spell | 说明 |
|---|---|
| 昆虫信息素 Insect Pheromone | 标记生物，使其被视为节肢生物 |
| 虫群护体 Swarm Aegis | 防护虫群减伤，攻击时召唤萤火虫助战 |
| 召唤蜂群 Summon Bee Swarm | 蜂群攻击你最后攻击过的目标，叠加蜂群之怒 |
| 混乱蜂刺 Chaotic Stinger | 召唤蜂蜇刺附带中毒/凋零 |
| 寄生虫 Parasite | 潜伏寄生，爆发时孵化蠹虫并引来虫群 |
| 蛛网缠绕 Web Entangle | 蛛网定身，可被破坏解救 |
| 小蜂刺 Bee Stinger | 高速蜂刺投射物 |
| 蜂蜂落幕曲 Bee Requiem | 三只自爆蜂俯冲目标 |
| 蜜蜂预警阵 Bee Alarm | 固定炮台蜂，自动攻击范围内敌人 |
| 蛛巢 Spider Nest | 周期性生成蜘蛛防御区域，附近野生蜘蛛也会加入协防 |
| 召唤冰霜蜘蛛 Summon Frost Spider | 可骑乘的冰霜蜘蛛战宠 |
| 召唤蟑螂舞队 Summon Cockroach Dance Troupe | 摇沙锤的蟑螂舞队提供节奏光环 |
| 召唤蚊群 Summon Mosquito Swarm | 绯红蚊子俯冲猎杀（优先玩家，其次敌对生物），吸血共生为附近召唤物回血 |

**装备与物品 Items**

- 虫群与我 Swarm Spell Book：虫群派系法术书
- 虫群·女王蜂皇冠 Queen Bee Crown：攻击使目标心醉魂迷（叠加易伤）
- 虫群·织网之蛛螯肢 Weaver Spider Chelicerae：虫类生物不再主动攻击你
- 虫群·蜜蜂化身 Bee Incarnation：召唤物击杀时可能孕育新的召唤物
- 虫群·群集之杖 / 虫群符文 / 虫群升级法球：施法法杖与升级材料
- 蜂王浆 / 蛛毒腺 / 虫水晶：虫群法术击杀掉落的合成材料

## 需求 Requirements

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.10+ |
| Iron's Spells 'n Spellbooks | 1.20.1-3.16.3 |
| GeckoLib | 1.20.1-4.8.4 |
| Curios | 5.14.1+ |
| Alex's Mobs（可选 optional） | 1.22.9 |

## 安装 Install

1. 安装 Forge 47.4.10+；
2. 将上方依赖与本模组 jar 一并放入 `mods` 文件夹；
3. 启动游戏，在法术书中寻找「虫群」派系。

## 计划更新 Todo

| # | 内容 | 效果 |
|---|------|------|
| 1 | 虫群豁免 Swarm Exemption* | 新 effect + 法术，对特定目标释放，使虫子无视友军保护可进攻（解决内战无法互攻的问题） |
| 2 | 蜜影迷踪 Honey Courier* | 召唤一只不攻击的蜜蜂，8~2s（随法术等级提高而降低）后回到施法者身边，恢复生命值、魔力值及一定饱食度后消失 |
| 3 | 寄生气息 Parasitic Breath* | 蠹虫专属效果（类似混乱蜂刺）：被召唤的蠹虫造成饥饿、缓慢（可叠加） |
| 3.5 | 混乱蜂刺 Chaotic Stinger | 中毒/凋零改为可叠加 |
| 4 | 蜂群呼唤 Swarm Call* | 给予自身同名 buff，受击时有概率召唤蜜蜂回击进攻者 |
| 5 | 女王威严 Queen's Majesty* | 使 32 格内所有自身召唤物获得抗性1、生命恢复1、力量2、速度1、急迫1，造成的伤害提高 20% |

\* 为暂定英文名，实装时可能调整。

## 许可证 License

本项目采用 **GNU General Public License v3.0 (GPL-3.0)**。

本许可证要求源自：本模组的代码直接继承并复用了 [Alex's Mobs](https://github.com/AlexModGuy/AlexsMobs)（GPL-3.0-only）的实体类（`EntityCockroach`、`RenderCockroach`，以及自 v1.3.0 起的 `EntityCrimsonMosquito`、`RenderCrimsonMosquito`），根据 GPL-3.0 的传染性条款，衍生作品必须以 GPL-3.0 发布。

完整许可证文本见 [LICENSE](./LICENSE) 文件。

### 依赖许可证

| 依赖 | 许可证 |
|---|---|
| Alex's Mobs | GPL-3.0-only |
| Iron's Spells 'n Spellbooks | All Rights Reserved（允许作为依赖编写 addon） |
| GeckoLib | MIT |
| Curios | MIT |
