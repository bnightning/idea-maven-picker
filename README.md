# IDEA Maven Picker

在多模块 Maven 工程中，通过可视化勾选模块，使用 IntelliJ 内置 Maven Runner 执行选择性构建。

## 功能

- **模块树勾选**：扫描 Maven 模块，按分组展示，支持搜索与批量选择
- **多种 Goal**：`clean`、`test`、`verify`、`package`、`install`、`deploy` 及自定义 Goal
- **命令预览与复制**：实时预览 Maven 命令，一键复制到剪贴板
- **常用参数**：`-am`、`-amd`、跳过测试、离线模式、Profiles、额外参数
- **预设管理**：可新增 / 编辑 / 删除预设，支持 JSON 导入导出
- **Git 变更模块**：基于 `git diff` 一键勾选受影响模块
- **多 Reactor 支持**：跨 Reactor 根时分批顺序执行
- **依赖提示**：启用 `-am` 时显示将额外构建的上游模块
- **最近使用**：保存最近 10 次构建配置，快速恢复
- **自动刷新**：Maven 导入或 POM 变更后自动更新模块树
- **右键联动**：在项目视图或编辑器中对模块 / `pom.xml` 右键加入选择
- **选项持久化**：记住勾选状态、Goal、参数与预设

## 兼容性

| 项目 | 要求 |
|------|------|
| IntelliJ IDEA | 2023.3 及以上（build `233` ~ `251.*`） |
| Maven | 需已安装 IntelliJ Maven 插件（自带） |
| Git | Git 变更模块功能需系统已安装 `git` 命令 |
| 项目类型 | 多模块 Maven 工程 |

## 安装

### 从 Release 安装

1. 前往 [Releases](https://github.com/bnightning/idea-maven-picker/releases) 下载最新 `.zip` 插件包
2. 打开 IDEA → **Settings / Preferences** → **Plugins** → ⚙️ → **Install Plugin from Disk…**
3. 选择下载的 zip 文件，重启 IDE

### 从源码构建

```bash
git clone https://github.com/bnightning/idea-maven-picker.git
cd idea-maven-picker
./build-plugin.sh
```

产物位于 `build/distributions/*.zip`，按上述方式从磁盘安装即可。

## 使用

1. 打开一个多模块 Maven 项目
2. 通过以下任一方式打开工具窗口：
   - 右侧工具栏 **Maven Picker**
   - 菜单 **Tools → Maven Picker → Selective Package**
3. 勾选需要构建的模块（可用预设、Git 变更、搜索辅助）
4. 选择 Goal 与构建参数
5. 点击 **执行**，或按 `Ctrl+Enter`

### 快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl+Enter` | 执行当前 Goal |
| `Ctrl+Shift+C` | 复制命令预览 |

### 预设导入导出

在 **预设 → 管理** 对话框中，可导出 JSON 到剪贴板，或从 JSON 导入团队共享预设。

## 开发

环境要求：JDK 17、Gradle（项目自带 wrapper）

```bash
# 构建插件
./gradlew buildPlugin

# 运行测试
./gradlew test

# 在沙箱 IDE 中调试
./gradlew runIde
```

## 发布

推送版本 tag 后，GitHub Actions 会自动构建并发布 Release：

```bash
git tag v1.0.0
git push origin v1.0.0
```

## License

本项目采用 [MIT License](LICENSE) 开源。
