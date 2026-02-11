#! /bin/bash -e

for dir in realguardio-*/ end-to-end-tests; do
  if [ -f "$dir/build.gradle" ]; then
    echo "running in $dir..."
    (cd "$dir" && "$@" ) || exit 1
  fi
done
