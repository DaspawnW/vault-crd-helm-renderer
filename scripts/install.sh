#!/bin/sh

set -e
set -o pipefail

VERSION="$1"

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Java 21 or higher is required. Current version: $JAVA_VERSION"
    echo "Please install Java 21 or later, then uninstall and install again."
    exit 1
fi

if [ $# -eq 0 ]; then
  VERSION=$(curl -sf https://api.github.com/repos/DaspawnW/vault-crd-helm-renderer/releases/latest | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/')
fi
curl -Lf -o vault-crd-helm-renderer.jar "https://github.com/DaspawnW/vault-crd-helm-renderer/releases/download/${VERSION}/vault-crd-helm-renderer.jar"
