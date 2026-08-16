#!/bin/bash
#
# Assembles and publishes the @ldbc/mcp-document-server npm package.
#
# Expects the Scala.js output to be linked beforehand:
#   sbt mcpDocumentServer/Compile/fastOptJS
#   sbt docs/tlSite
#
# Usage:
#   script/publish-npm-package.sh            # assemble and publish
#   script/publish-npm-package.sh --pack     # assemble and npm pack only (no publish)
#
# NPM_TOKEN must be set when publishing.

set -euo pipefail

# Directory paths
PROJECT_DIR="mcp/document-server/.js"
STAGING_DIR="$PROJECT_DIR/target/npm"
MDOC_DIR="docs/target/mdoc"

PACK_ONLY=false
if [ "${1:-}" = "--pack" ]; then
  PACK_ONLY=true
fi

# Locate the linker output. The scala-<version> segment varies with the build,
# so it is resolved by glob rather than hard-coded.
LINKER_DIR=$(find "$PROJECT_DIR/target" -type d -name "mcp-ldbc-document-server-fastopt" | head -1)

if [ -z "$LINKER_DIR" ]; then
  echo "Linker output not found under $PROJECT_DIR/target." >&2
  echo "Run 'sbt mcpDocumentServer/Compile/fastOptJS' first." >&2
  exit 1
fi

if [ ! -f "$LINKER_DIR/main.js" ]; then
  echo "$LINKER_DIR/main.js is missing." >&2
  exit 1
fi

# The bin entry relies on the hashbang injected via scalaJSLinkerConfig.
if ! head -1 "$LINKER_DIR/main.js" | grep -q '^#!/usr/bin/env node'; then
  echo "main.js is missing the '#!/usr/bin/env node' hashbang." >&2
  echo "Check the jsHeader setting on mcpDocumentServer in build.sbt." >&2
  exit 1
fi

echo "Assembling package from $LINKER_DIR"

rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"

cp "$LINKER_DIR/main.js" "$STAGING_DIR/main.js"
if [ -f "$LINKER_DIR/main.js.map" ]; then
  cp "$LINKER_DIR/main.js.map" "$STAGING_DIR/main.js.map"
fi

cp "$PROJECT_DIR/package.json" "$STAGING_DIR/package.json"
cp "$PROJECT_DIR/README.md" "$STAGING_DIR/README.md"

# The document server serves the mdoc output, so it ships inside the package.
if [ -d "$MDOC_DIR" ]; then
  cp -R "$MDOC_DIR" "$STAGING_DIR/docs"
  echo "Copied documentation from $MDOC_DIR"
else
  echo "Documentation not found at $MDOC_DIR." >&2
  echo "Run 'sbt docs/tlSite' first." >&2
  exit 1
fi

if [ "$PACK_ONLY" = true ]; then
  (cd "$STAGING_DIR" && npm pack)
  echo "Packed into $STAGING_DIR"
  exit 0
fi

if [ -z "${NPM_TOKEN:-}" ]; then
  echo "NPM_TOKEN is not set." >&2
  exit 1
fi

echo "//registry.npmjs.org/:_authToken=\${NPM_TOKEN}" > "$STAGING_DIR/.npmrc"

(cd "$STAGING_DIR" && npm publish)

echo "Published $(node -p "require('./$STAGING_DIR/package.json').name")@$(node -p "require('./$STAGING_DIR/package.json').version")"
