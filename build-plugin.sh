#!/usr/bin/env bash
set -euo pipefail

# 构建 IDEA 插件 zip 的脚本
# 默认使用 build.gradle.kts 里定义的 version（当前为 0.0.2-alpha）
#
# 可选：通过 --version/-v 覆盖版本（例如：./build-plugin.sh -v 1.2.3-beta）

VERSION=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version)
      VERSION="${2:-}"
      shift 2
      ;;
    -h|--help)
      echo "用法: $0 [--version|-v <version>]"
      exit 0
      ;;
    *)
      echo "未知参数: $1"
      echo "用法: $0 [--version|-v <version>]"
      exit 1
      ;;
  esac
done

cd "$(dirname "$0")"

chmod +x ./gradlew

if [[ -n "$VERSION" ]]; then
  echo "构建插件（version=$VERSION）..."
  ./gradlew --no-daemon clean buildPlugin -PpluginVersion="$VERSION"
else
  echo "构建插件（使用默认 version）..."
  ./gradlew --no-daemon clean buildPlugin
fi

echo "构建完成。产物位置："
echo "  build/distributions/*.zip"
ls -lah build/distributions/*.zip || true

