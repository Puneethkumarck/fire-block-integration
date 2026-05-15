#!/bin/bash
set -euo pipefail

REGION="us-east-1"

PRIVATE_KEY_PEM=$(openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 2>/dev/null)

DUMMY_PRIVATE_KEY=$(echo "$PRIVATE_KEY_PEM" \
  | openssl pkcs8 -topk8 -nocrypt -outform DER 2>/dev/null \
  | base64 -w0)

awslocal secretsmanager create-secret \
  --name "fireblocks/private-key" \
  --secret-string "$DUMMY_PRIVATE_KEY" \
  --region "$REGION"

awslocal secretsmanager create-secret \
  --name "fireblocks/api-key" \
  --secret-string "dummy-fireblocks-api-key-for-local-dev" \
  --region "$REGION"

DUMMY_WEBHOOK_PUBLIC_KEY=$(echo "$PRIVATE_KEY_PEM" \
  | openssl rsa -pubout -outform DER 2>/dev/null \
  | base64 -w0)

awslocal secretsmanager create-secret \
  --name "fireblocks/webhook-public-key" \
  --secret-string "$DUMMY_WEBHOOK_PUBLIC_KEY" \
  --region "$REGION"

echo "LocalStack secrets initialized successfully"
