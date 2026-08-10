#!/usr/bin/env bash
set -euo pipefail

REPO_URL="https://github.com/khalidbourr/B2XKlaim.git"
DEPLOY_BRANCH="deploy"

WEB_ROOT="$HOME/kbourr.com"
FRONTEND_DEST="$WEB_ROOT/b2xklaim"
BACKEND_DEST="$WEB_ROOT/b2api"
TMP_DIR="/tmp/b2xklaim-deploy"

git clone --depth 1 -b "$DEPLOY_BRANCH" "$REPO_URL" "$TMP_DIR" || { echo "Failed to clone $REPO_URL branch $DEPLOY_BRANCH"; exit 1; }

echo "Frontend: $FRONTEND_DEST"
echo "Backend : $BACKEND_DEST"

if [ -d "$TMP_DIR/b2xklaim" ]; then
    rm -rf "$FRONTEND_DEST"
    cp -r "$TMP_DIR/b2xklaim" "$FRONTEND_DEST"
    echo "Frontend deployed."
else
    echo "No b2xklaim/ dir in deploy branch"
fi

JAR=""
for f in "$TMP_DIR"/b2api/B2XKlaim-*.jar; do
    [ -f "$f" ] && JAR="$f" && break
done

if [ -n "$JAR" ]; then
    cp "$JAR" "$BACKEND_DEST/"
    echo "Jar deployed: $(basename "$JAR")"
    if [ -f "$BACKEND_DEST/app-control.sh" ]; then
        "$BACKEND_DEST/app-control.sh" restart || echo "app-control.sh restart failed (manual restart may be needed)"
    else
        echo "app-control.sh not found; restart the backend manually."
    fi
else
    echo "No jar found in deploy branch b2api/"
fi

rm -rf "$TMP_DIR"
echo "Done."