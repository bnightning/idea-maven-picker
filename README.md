# IDEA Maven Picker

在多模块 Maven 工程中，通过可视化勾选模块，使用 IntelliJ 内置 Maven Runner 执行选择性打包。

## 功能

- **模块树勾选**：扫描当前项目的 Maven 模块，按分组展示，支持勾选叶子服务模块
- **命令预览**：实时预览将要执行的 Maven 命令（`mvn clean package -pl <模块> -am`）
- **一键执行**：支持独立 **Clean**、**Package**，或 Package 前先 clean
- **批量操作**：全选叶子、清空、反选
- **预设筛选**：快速选择「叶子服务」「core 组」「services 组」「modules 组」
- **选项持久化**：记住勾选状态与常用选项（`-am`、显示聚合 POM 等）

## 兼容性

| 项目 | 要求 |
|------|------|
| IntelliJ IDEA | 2023.3 及以上（build `233` ~ `251.*`） |
| Maven | 需已安装 IntelliJ Maven 插件（自带） |
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

指定版本号构建：

```bash
./build-plugin.sh --version 1.0.0
```

## 使用

1. 打开一个多模块 Maven 项目
2. 通过以下任一方式打开工具窗口：
   - 右侧工具栏 **Maven Picker**
   - 菜单 **Tools → Maven Picker → Selective Package**
3. 在模块树中勾选需要打包的模块
4. 根据需要调整选项：
   - **Package 前先 clean**：勾选后 Package 会先执行 clean
   - **显示聚合模块（pom）**：是否在树中显示 packaging=pom 的聚合模块
   - **-am 同时构建依赖/父 POM**：等价于 Maven `-am` 参数
5. 点击 **Package** 执行打包，或单独点击 **Clean**

命令将通过 IntelliJ 内置 Maven Runner 在对应 reactor 根目录下执行。

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

支持 `v1.0.0` 或 `1.0.0` 格式的 tag。

## License

本项目采用 [MIT License](LICENSE) 开源。
