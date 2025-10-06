# 大貂名片 (DiaoNameCard)

[![作者](https://img.shields.io/badge/作者-止-blue.svg)](https://github.com/ZHI-CCC)
[![团队](https://img.shields.io/badge/团队-大貂Team-orange.svg)](https://github.com/Giant-Diao-Team)
[![版本](https://img.shields.io/badge/版本-1.0.0-brightgreen.svg)](https://github.com/Giant-Diao-Team/DiaoNameCard)
本插件备注由 CodeGeeX 生成 （自动生成备注是真tmd好用）

一款为 Minecraft 服务器设计的、高度可定制的大貂化名片系统插件，允许玩家收集、展示和佩戴独特的个人名片。

## ✨ 特色功能

- **🎨 高度可定制**: 管理员可以通过配置文件轻松添加、修改或删除任意数量的名片，自定义每张名片的贴图、展示名和描述。
- **💾 多种存储方式**: 支持 `SQLite` (默认，轻量便捷) 和 `MySQL` (适合群组服务器) 两种数据存储方式，可在配置文件中一键切换 ⚠️目前还不支持数据转换！。
- **🎮 玩家互动**: 玩家可以自由选择佩戴自己已拥有的名片，向其他玩家展示自己的独特身份。
- **🔧 强大的管理命令**: 提供完整的后台命令，方便管理员重载配置、授予或移除玩家的名片。
- **🚀 异步数据处理**: 所有数据库操作均采用异步处理，最大程度减少对服务器主线程的影响，保证服务器流畅运行。
- **🔌 API 驱动**: 专门设计用于UI (CloudPick) 联动，实现云拾界面的名片信息展示。

## 📥 安装与依赖

1. **下载插件**:
   - 前往 [**发布页面**](https://github.com/Giant-Diao-Team/DiaoNameCard/releases) 下载最新的 `DiaoNameCard-x.x.x.jar` 文件。

2. **安装核心插件**:
   - 将下载的 `DiaoNameCard-x.x.x.jar` 文件放入您服务器的 `plugins` 文件夹。

3. **安装前置依赖**:
   - **[必需]** 本插件依赖 **`CloudPick`** 插件进行客户端通信。请确保您的 `plugins` 文件夹中已经安装了 `CloudPick-Bukkit-x.x.x.jar`。

4. **启动服务器**:
   - 启动您的 Spigot/Paper 1.20.1+ 服务器。首次启动后，插件会自动在 `plugins/DiaoNameCard` 目录下生成默认的 `config.yml` 配置文件。

## 📜 命令与权限

| 命令 | 描述 | 权限 |
| :--- | :--- | :--- |
| `/dnc reload` | 重载插件的配置文件。 | `diaonamecard.admin.reload` |
| `/dnc add <玩家> <名片ID>` | 给予指定玩家一张名片。 | `diaonamecard.admin.add` |
| `/dnc remove <玩家> <名片ID>` | 移除指定玩家的一张名片。 | `diaonamecard.admin.remove` |
| `/dnc set <名片ID>` | 玩家选择并佩戴自己拥有的名片。 | `diaonamecard.player.set` |

**提示**: `OP` 默认拥有所有管理员权限。

## 🤝 UI界面通信 通信

本插件通过 `CloudPick` API 与客户端进行数据交互，以实现自定义界面的名片展示。

- **请求当前佩戴的名片**:
  - 客户端发送标识符: `dnc_name_card`
  - 插件返回标识符: `dnc_card`
  - 返回内容: `贴图路径`, `展示名`, `描述`

- **请求拥有的名片列表**:
  - 客户端发送标识符: `dnc_name_card_list`
  - 插件返回标识符: `dnc_card_list` (会为每张名片单独发送一次)
  - 返回内容: `名片ID`, `贴图路径`, `展示名`, `描述` (按 `layer` 从小到大排序)

## ❓ 常见问题 (FAQ)

- **Q: 插件加载时提示未找到 `CloudPick` 怎么办?**
  - **A:** 请确保您已经将 `CloudPick-Bukkit` 的 jar 文件正确放入了服务器的 `plugins` 文件夹，并且其版本与您的服务端兼容。

- **Q: 我修改了 `config.yml`，如何让它生效?**
  - **A:** 在游戏中或后台输入 `/dnc reload` 命令即可，无需重启服务器。

## 📝 参与贡献

欢迎任何形式的贡献！如果您发现了 Bug 或有任何好的建议，请随时提交 [**Issue**](https://github.com/Giant-Diao-Team/DiaoNameCard/issues)。
---

**大貂名片 (DiaoNameCard)** - 让你的服务器拥有超大貂的名片系统！
