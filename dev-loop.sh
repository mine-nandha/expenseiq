#!/usr/bin/env bash
#
# dev-loop.sh — watch ExpenseIQ source, rebuild, install, and relaunch on
# every change via adb. Gives clear, colorized feedback throughout.
#
# Usage:
#   ./dev-loop.sh                    watch + build + install + relaunch
#   ./dev-loop.sh --once             single build/install cycle, no watch
#   ./dev-loop.sh --clean            uninstall before install (wipes app data)
#   ./dev-loop.sh --notify           desktop notifications via notify-send
#   ./dev-loop.sh --no-start         install but do not launch the app
#   ./dev-loop.sh --serial <serial>  target a specific adb device
#
# While watching, press: r = rebuild now, s = stop command, p = pause,
# q = quit, l = clear screen. See `watchexec --help` for the full key map.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ID="com.mine.expenseiq"
ACTIVITY="com.mine.expenseiq/.MainActivity"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

DEBOUNCE_MS=1200
CLEAN=0
NOTIFY=0
NO_START=0
ONCE=0
TRIGGERED=0
SERIAL=""

# --- colors -----------------------------------------------------------------
if [[ -t 1 ]]; then
  C_RESET=$'\e[0m'
  C_BOLD=$'\e[1m'
  C_DIM=$'\e[2m'
  C_RED=$'\e[31m'
  C_GREEN=$'\e[32m'
  C_YELLOW=$'\e[33m'
  C_BLUE=$'\e[34m'
  C_MAGENTA=$'\e[35m'
  C_CYAN=$'\e[36m'
else
  C_RESET="" C_BOLD="" C_DIM="" C_RED="" C_GREEN="" C_YELLOW="" C_BLUE="" C_MAGENTA="" C_CYAN=""
fi

log()   { printf '%s[%s]%s %s\n' "$C_BLUE" "$(date +%H:%M:%S)" "$C_RESET" "$*"; }
ok()    { printf '%s%s✓ %s%s\n' "$C_GREEN" "$C_BOLD" "$*" "$C_RESET"; }
warn()  { printf '%s%s⚠ %s%s\n' "$C_YELLOW" "$C_BOLD" "$*" "$C_RESET"; }
err()   { printf '%s%s✗ %s%s\n' "$C_RED" "$C_BOLD" "$*" "$C_RESET" >&2; }
banner(){ printf '%s────────────────────────────────────────────────────────────%s\n' "$C_DIM" "$C_RESET"; }

# --- args -------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --once)      ONCE=1; shift ;;
    --clean)     CLEAN=1; shift ;;
    --notify)    NOTIFY=1; shift ;;
    --no-start)  NO_START=1; shift ;;
    --serial)    SERIAL="${2:-}"; shift 2 ;;
    --triggered) TRIGGERED=1; shift ;;
    -h|--help)
      sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
      exit 0 ;;
    *) warn "ignoring unknown argument: $1"; shift ;;
  esac
done

ADB=(adb)
[[ -n "$SERIAL" ]] && ADB=(adb -s "$SERIAL")

# --- notifications -----------------------------------------------------------
notify() {
  [[ "$NOTIFY" -eq 1 ]] || return 0
  command -v notify-send >/dev/null 2>&1 || return 0
  notify-send --urgency="$1" --app-name="dev-loop" "$2" "$3" >/dev/null 2>&1 || true
}

# --- device + deploy ----------------------------------------------------------
find_device() {
  local line state
  while IFS= read -r line; do
    state=$(awk '{print $2}' <<<"$line")
    if [[ "$state" == "device" ]]; then
      ok "device online: $(awk '{print $1}' <<<"$line")"
      return 0
    fi
  done < <("${ADB[@]}" devices 2>/dev/null | tail -n +2)
  return 1
}

wait_for_device() {
  local attempts=0
  until find_device; do
    attempts=$((attempts + 1))
    if [[ "$attempts" -eq 1 ]]; then
      warn "no adb device online — plug in your phone and authorize USB debugging, press 'r' to retry"
      notify normal "dev-loop" "Waiting for device…"
    fi
    if [[ "$attempts" -ge 30 ]]; then
      err "no device after 30s — plug in your phone, then press 'r' in the watcher"
      return 1
    fi
    sleep 1
  done
}

install_apk() {
  if [[ "$CLEAN" -eq 1 ]]; then
    log "uninstalling $APP_ID (wipes app data)"
    "${ADB[@]}" uninstall "$APP_ID" >/dev/null 2>&1 || true
  fi
  log "installing: $APK_PATH"
  "${ADB[@]}" install -r "$APK_PATH"
}

relaunch() {
  if [[ "$NO_START" -eq 1 ]]; then
    log "skipping launch (--no-start)"
    return 0
  fi
  "${ADB[@]}" shell am force-stop "$APP_ID" >/dev/null
  if "${ADB[@]}" shell am start -n "$ACTIVITY" >/dev/null; then
    ok "launched $ACTIVITY"
  else
    err "failed to start $ACTIVITY"
  fi
}

build_and_deploy() {
  local reason="${1:-change}"

  banner
  log "rebuild triggered: $reason"
  if ! wait_for_device; then
    banner
    return
  fi

  banner
  log "building debug APK ($C_DIM./gradlew :app:assembleDebug$C_RESET)"
  local t0 t1 code CANCELLED timer_pid
  t0=$(date +%s)
  CANCELLED=0
  timer_pid=""

  ( while true; do
      printf '\r\033[2K  %s⏱ assembleDebug %ss%s' \
        "$C_CYAN" "$(( $(date +%s) - t0 ))" "$C_DIM" >&2
      sleep 1
    done ) &
  timer_pid=$!
  trap 'CANCELLED=1; [[ -n "$timer_pid" ]] && kill "$timer_pid" 2>/dev/null || true' INT TERM

  if (cd "$PROJECT_DIR" && ./gradlew :app:assembleDebug); then
    code=0
  else
    code=$?
  fi

  [[ -n "$timer_pid" ]] && kill "$timer_pid" 2>/dev/null || true
  wait "$timer_pid" 2>/dev/null || true
  printf '\r\033[2K' >&2
  trap - INT TERM

  t1=$(date +%s)
  if [[ "$CANCELLED" -eq 1 ]]; then
    warn "build cancelled — new change detected, restarting"
    banner
    return
  fi
  if [[ "$code" -ne 0 ]]; then
    err "BUILD FAILED (gradle exit $code) after $((t1 - t0))s — errors above"
    notify critical "dev-loop: build failed" "gradle exit $code after $((t1 - t0))s"
    banner
    return
  fi

  ok "build succeeded in $((t1 - t0))s"

  if [[ ! -f "$APK_PATH" ]]; then
    err "APK not found at $APK_PATH"
    banner
    return
  fi

  banner
  if install_apk; then
    ok "install succeeded"
    notify normal "dev-loop: installed" "New build installed on device"
  else
    err "INSTALL FAILED — signature mismatch, or device storage issue"
    notify critical "dev-loop: install failed" "adb install rejected the APK"
    banner
    return
  fi

  banner
  relaunch
  banner
  printf '%s» %s watching for changes %s(r=rebuild s=stop p=pause q=quit l=clear)%s @ %s\n' \
    "$C_GREEN" "$C_MAGENTA" "$C_DIM" "$C_RESET" "$(date +%H:%M:%S)"
}

# --- main ---------------------------------------------------------------------
if [[ "$TRIGGERED" -eq 1 ]]; then
  build_and_deploy "${WATCHEXEC_UPDATED_PATHS:-change}"
  exit 0
fi

LOCK_FILE="$PROJECT_DIR/.dev-loop.lock"
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  err "another dev-loop is already running ($LOCK_FILE)"
  exit 1
fi

banner
log "ExpenseIQ dev loop — app: $APP_ID"
[[ -n "$SERIAL" ]] && log "target device: $SERIAL"
[[ "$CLEAN" -eq 1 ]]  && log "clean reinstall: app data is wiped each cycle"
[[ "$NOTIFY" -eq 1 ]] && log "desktop notifications: enabled"
banner

if [[ "$ONCE" -eq 1 ]]; then
  build_and_deploy "manual --once run"
  exit 0
fi

log "watching for changes in $PROJECT_DIR…"

INNER_FLAGS="--triggered"
[[ "$CLEAN" -eq 1 ]]     && INNER_FLAGS+=" --clean"
[[ "$NOTIFY" -eq 1 ]]    && INNER_FLAGS+=" --notify"
[[ "$NO_START" -eq 1 ]]  && INNER_FLAGS+=" --no-start"
[[ -n "$SERIAL" ]]       && INNER_FLAGS+=" --serial $SERIAL"

exec watchexec -I \
  --shell bash \
  -d "${DEBOUNCE_MS}ms" \
  -o restart \
  --stop-timeout 3s \
  --project-origin "$PROJECT_DIR" \
  -w "$PROJECT_DIR/app/src" \
  -w "$PROJECT_DIR/app/build.gradle.kts" \
  -w "$PROJECT_DIR/build.gradle.kts" \
  -w "$PROJECT_DIR/settings.gradle.kts" \
  -w "$PROJECT_DIR/gradle" \
  -w "$PROJECT_DIR/dev-loop.sh" \
  -- bash "$PROJECT_DIR/dev-loop.sh" $INNER_FLAGS