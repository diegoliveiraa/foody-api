#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

secrets_dir=".secrets"
private_key="$secrets_dir/jwt-private.pem"
public_key="$secrets_dir/jwt-public.pem"

if [ "${1:-}" != "--force" ] && { [ -e "$private_key" ] || [ -e "$public_key" ]; }; then
	echo "JWT keys already exist in $secrets_dir — run with --force to replace them (every issued token stops working)."
	exit 1
fi

mkdir -p "$secrets_dir"
openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$private_key"
openssl pkey -in "$private_key" -pubout -out "$public_key"
chmod 644 "$private_key" "$public_key"
echo "JWT key pair written to $private_key and $public_key."
