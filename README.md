# vault-crd-helm-renderer

## Requirements

* Java JRE 21+
* Helm v3+
* jq & curl (for automated plugin install with helm v4)

## Install & usage

### With helm v4:

Install:
```bash
helm plugin install --verify=false https://github.com/camaeel/vault-crd-helm-renderer
```

Usage:
```shell
helm upgrade --install my-chart ./my-chart \
  --post-renderer vault-crd-helm-renderer
```

### Helm v3

Install:
```shell
HELM_PLUGIN_DIR=. scripts/install.sh 
```


Usage:
```shell
helm upgrade --install my-chart ./my-chart --post-renderer java --post-renderer-args -jar --post-renderer-args vault-crd-helm-renderer.jar 
```