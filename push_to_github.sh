#!/bin/bash
# ============================================================
# TerraBreed Android - Push Fixed Files to GitHub
# Jalankan script ini di terminal lokal Anda
# ============================================================

TOKEN="YOUR_GITHUB_PAT_HERE"
REPO="Kendo-id/terrabreed-android"
API="https://api.github.com/repos/$REPO/contents"

# Deteksi folder script ini
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/terrabreed-fixed"

if [ ! -d "$SRC" ]; then
    echo "❌ Folder terrabreed-fixed tidak ditemukan!"
    echo "   Pastikan Anda extract zip di folder yang sama dengan script ini."
    exit 1
fi

push_file() {
    local api_path="$1"
    local file="$2"
    local msg="$3"

    if [ ! -f "$file" ]; then
        echo "⚠️  Skip (not found): $api_path"
        return
    fi

    local content=$(base64 -w 0 "$file" 2>/dev/null || base64 "$file")
    local sha=$(curl -s -H "Authorization: token $TOKEN" \
        "$API/$api_path" | python3 -c \
        "import sys,json; d=json.load(sys.stdin); print(d.get('sha',''))" 2>/dev/null)

    local body
    if [ -n "$sha" ]; then
        body=$(python3 -c "
import json, sys
print(json.dumps({'message': sys.argv[1], 'content': sys.argv[2], 'sha': sys.argv[3]}))" \
"$msg" "$content" "$sha")
    else
        body=$(python3 -c "
import json, sys
print(json.dumps({'message': sys.argv[1], 'content': sys.argv[2]}))" \
"$msg" "$content")
    fi

    local result=$(curl -s -X PUT \
        -H "Authorization: token $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$body" "$API/$api_path")

    if echo "$result" | grep -q '"commit"'; then
        echo "✅ $api_path"
    else
        local err=$(echo "$result" | python3 -c \
            "import sys,json; d=json.load(sys.stdin); print(d.get('message','?'))" 2>/dev/null)
        echo "❌ $api_path → $err"
    fi
}

echo ""
echo "🚀 TerraBreed Android — Pushing fixes to GitHub..."
echo "📦 Repo: $REPO"
echo ""

push_file "app/src/main/java/com/terrabreed/app/api/ApiClient.kt" \
    "$SRC/app/src/main/java/com/terrabreed/app/api/ApiClient.kt" \
    "fix: default IP 10.10.1.1, HTTPS + trust all local certs"

push_file "app/src/main/java/com/terrabreed/app/activities/SettingsActivity.kt" \
    "$SRC/app/src/main/java/com/terrabreed/app/activities/SettingsActivity.kt" \
    "fix: HTTPS toggle, fix default server IP"

push_file "app/src/main/java/com/terrabreed/app/activities/SplashActivity.kt" \
    "$SRC/app/src/main/java/com/terrabreed/app/activities/SplashActivity.kt" \
    "feat: splash screen with default config"

push_file "app/src/main/java/com/terrabreed/app/fragments/AiChatFragment.kt" \
    "$SRC/app/src/main/java/com/terrabreed/app/fragments/AiChatFragment.kt" \
    "feat: new AI Chat fragment (TERRA AI)"

push_file "app/src/main/java/com/terrabreed/app/fragments/ControlFragment.kt" \
    "$SRC/app/src/main/java/com/terrabreed/app/fragments/ControlFragment.kt" \
    "feat: new Control fragment - actuator, target, tray"

push_file "app/src/main/java/com/terrabreed/app/fragments/DashboardFragment.kt" \
    "$SRC/app/src/main/java/com/terrabreed/app/fragments/DashboardFragment.kt" \
    "feat: dashboard chips clickable for actuator control"

push_file "app/src/main/java/com/terrabreed/app/MainActivity.kt" \
    "$SRC/app/src/main/java/com/terrabreed/app/MainActivity.kt" \
    "feat: nav Control + AI Chat"

push_file "app/src/main/res/layout/fragment_ai_chat.xml" \
    "$SRC/app/src/main/res/layout/fragment_ai_chat.xml" \
    "feat: AI Chat layout"

push_file "app/src/main/res/layout/fragment_control.xml" \
    "$SRC/app/src/main/res/layout/fragment_control.xml" \
    "feat: Control layout"

push_file "app/src/main/res/layout/activity_settings.xml" \
    "$SRC/app/src/main/res/layout/activity_settings.xml" \
    "fix: settings layout + HTTPS toggle"

push_file "app/src/main/res/layout/activity_splash.xml" \
    "$SRC/app/src/main/res/layout/activity_splash.xml" \
    "feat: splash layout"

push_file "app/src/main/res/menu/bottom_nav_menu.xml" \
    "$SRC/app/src/main/res/menu/bottom_nav_menu.xml" \
    "feat: Control + AI Chat bottom nav"

push_file "app/src/main/res/xml/network_security_config.xml" \
    "$SRC/app/src/main/res/xml/network_security_config.xml" \
    "fix: network security config for local HTTPS"

push_file "app/src/main/res/values/strings.xml" \
    "$SRC/app/src/main/res/values/strings.xml" \
    "feat: add new strings"

push_file "app/src/main/AndroidManifest.xml" \
    "$SRC/app/src/main/AndroidManifest.xml" \
    "fix: add networkSecurityConfig"

echo ""
echo "🎉 Selesai! Pantau build di:"
echo "   https://github.com/$REPO/actions"
