#! /bin/bash -e

for dir in realguardio-*/; do
  if [ -f "$dir/build.gradle" ]; then
    echo "running in $dir..."
    (cd "$dir" && "$@" ) || exit 1
  fi
done
