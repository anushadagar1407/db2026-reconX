#!/bin/sh
set -eu

case "${RECONX_DEMO_URL}" in
  http://*|https://*) ;;
  *)
    printf '%s\n' 'RECONX_DEMO_URL must use http or https.' >&2
    exit 1
    ;;
esac

config=$(jq -cn --arg demoUrl "${RECONX_DEMO_URL}" '{demoUrl: $demoUrl}')
printf 'window.__RECONX_PRESENTATION_CONFIG__ = Object.freeze(%s);\n' "${config}" \
  > /usr/share/nginx/html/config.js
