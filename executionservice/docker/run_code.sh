#!/bin/sh
# This script is NOT needed anymore - execution service uses inline scripts.
# Kept here as reference only.
LANG=$1
case $LANG in
  python)     python3 /workspace/main.py ;;
  javascript) node /workspace/main.js ;;
  java)       cd /tmp && cp /workspace/Main.java . && javac Main.java && java Main ;;
  c++)        cd /tmp && cp /workspace/main.cpp . && g++ -o main main.cpp && ./main ;;
  go)         cd /tmp && cp /workspace/main.go . && go run main.go ;;
  rust)       cd /tmp && cp /workspace/main.rs . && rustc -o main main.rs && ./main ;;
  *)          echo "Unsupported language: $LANG"; exit 1 ;;
esac
