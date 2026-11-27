#!/usr/bin/env bash
# debloat.sh — Désactiver/supprimer le bloatware Android via ADB (sans root).
# Usage :
#   ./debloat.sh list                 -> liste les paquets suspects présents
#   ./debloat.sh disable <paquet>     -> désactive un paquet (réversible)
#   ./debloat.sh restore <paquet>     -> restaure un paquet désactivé
#   ./debloat.sh uninstall <paquet>   -> désinstalle pour l'utilisateur 0 (réversible)
#   ./debloat.sh menu                 -> mode interactif par marque

set -u

ADB_OK=1
command -v adb >/dev/null 2>&1 || ADB_OK=0

die() { echo "Erreur : $1" >&2; exit 1; }

need_device() {
  [ "$ADB_OK" -eq 1 ] || die "adb introuvable. Installez 'platform-tools' : https://developer.android.com/tools/releases/platform-tools"
  adb get-state >/dev/null 2>&1 || die "Aucun appareil détecté. Activez le débogage USB et acceptez la fenêtre sur le téléphone."
}

brand() {
  adb shell getprop ro.product.brand 2>/dev/null | tr -d '\r' | tr '[:upper:]' '[:lower:]'
}

device_packages() {
  adb shell pm list packages | tr -d '\r' | sed 's/^package://'
}

# Listes de bloatware courant (SÛR = désactivation sans risque fonctionnel courant)
SAMSUNG_SAFE="
com.samsung.android.app.spage
com.samsung.android.game.gamehome
com.samsung.android.game.gametools
com.samsung.android.arzone
com.samsung.android.aremoji
com.samsung.android.aremojieditor
com.sec.android.mimage.avatarstickers
com.samsung.android.bixby.agent
com.samsung.android.bixby.wakeup
com.samsung.android.bixbyvision.framework
com.samsung.android.visionintelligence
com.samsung.android.tvplus
com.samsung.android.themestore
com.samsung.android.service.peoplestripe
com.samsung.android.app.taskedge
com.facebook.katana
com.facebook.appmanager
com.facebook.services
com.facebook.system
com.microsoft.appmanager
com.flipboard.app
"

XIAOMI_SAFE="
com.miui.player
com.miui.videoplayer
com.miui.yellowpage
com.miui.msa.global
com.miui.analytics
com.miui.hybrid
com.miui.hybrid.accessory
com.micredit.in
com.miui.miservice
com.miui.android.fashiongallery
com.xiaomi.midrop
com.xiaomi.mipicks
com.facebook.katana
com.facebook.appmanager
com.facebook.services
com.facebook.system
"

GENERIC_SAFE="
com.google.android.feedback
com.google.android.printservice.recommendation
com.google.android.apps.restore
com.google.android.apps.tachyon
"

list_for_brand() {
  local b; b="$(brand)"
  case "$b" in
    samsung) echo "$SAMSUNG_SAFE$GENERIC_SAFE" ;;
    xiaomi|redmi|poco) echo "$XIAOMI_SAFE$GENERIC_SAFE" ;;
    *) echo "$GENERIC_SAFE" ;;
  esac
}

cmd_list() {
  need_device
  echo "Marque détectée : $(brand)"
  echo "Paquets suspects présents sur l'appareil :"
  local pkgs present=0
  device_packages > /tmp/device_pkgs.txt
  while read -r p; do
    [ -z "$p" ] && continue
    if grep -qx "$p" /tmp/device_pkgs.txt; then
      echo "  $p"
      present=$((present+1))
    fi
  done < <(list_for_brand)
  [ "$present" -eq 0 ] && echo "  (aucun)"
}

cmd_disable() {
  need_device
  echo "Désactivation de $1…"
  adb shell pm disable-user --user 0 "$1" && echo "OK (réversible avec 'restore')"
}

cmd_restore() {
  need_device
  echo "Restauration de $1…"
  adb shell cmd package install-existing "$1" && echo "OK"
}

cmd_uninstall() {
  need_device
  echo "Désinstallation pour l'utilisateur 0 de $1…"
  adb shell pm uninstall -k --user 0 "$1" && echo "OK (réversible avec 'restore' tant que l'appareil n'est pas réinitialisé)"
}

cmd_menu() {
  need_device
  echo "Marque détectée : $(brand)"
  echo
  echo "Choisissez un niveau :"
  echo "  1) Désactiver les paquets SÛRS (recommandé, réversible)"
  echo "  2) Restaurer tous les paquets SÛRS précédemment désactivés"
  echo "  3) Quitter"
  read -r -p "> " choice
  case "$choice" in
    1)
      while read -r p; do
        [ -z "$p" ] && continue
        if grep -qx "$p" /tmp/device_pkgs.txt || device_packages | grep -qx "$p"; then
          echo "Désactivation : $p"
          adb shell pm disable-user --user 0 "$p" >/dev/null 2>&1 && echo "  OK" || echo "  échec"
        fi
      done < <(list_for_brand)
      ;;
    2)
      while read -r p; do
        [ -z "$p" ] && continue
        echo "Restauration : $p"
        adb shell cmd package install-existing "$p" >/dev/null 2>&1 && echo "  OK" || echo "  absent"
      done < <(list_for_brand)
      ;;
    *) echo "Au revoir." ;;
  esac
}

case "${1:-menu}" in
  list)      cmd_list ;;
  disable)   [ $# -ge 2 ] || die "usage : $0 disable <paquet>"; cmd_disable "$2" ;;
  restore)   [ $# -ge 2 ] || die "usage : $0 restore <paquet>"; cmd_restore "$2" ;;
  uninstall) [ $# -ge 2 ] || die "usage : $0 uninstall <paquet>"; cmd_uninstall "$2" ;;
  menu)      cmd_menu ;;
  *)         echo "usage : $0 {list|disable|restore|uninstall|menu} [paquet]"; exit 2 ;;
esac
