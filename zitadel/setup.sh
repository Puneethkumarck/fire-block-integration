#!/bin/bash
set -euo pipefail

ZITADEL_URL="http://localhost:8085"
MACHINE_KEY_FILE="./zitadel/data/machine-key.json"
PROJECT_NAME="custody-fireblocks"

base64url() {
  openssl enc -base64 -A | tr '+/' '-_' | tr -d '='
}

echo "Waiting for Zitadel to be ready..."
until curl -sf "$ZITADEL_URL/debug/ready" > /dev/null 2>&1; do
  sleep 2
done
echo "Zitadel is ready"

echo "Waiting for machine key file..."
for i in $(seq 1 30); do
  if [ -f "$MACHINE_KEY_FILE" ] && [ -s "$MACHINE_KEY_FILE" ]; then
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "ERROR: Machine key file not found at $MACHINE_KEY_FILE after 60s."
    echo "Ensure Zitadel started with ZITADEL_FIRSTINSTANCE_MACHINEKEYPATH configured."
    exit 1
  fi
  sleep 2
done
echo "Machine key found"

KEY_ID=$(jq -r '.keyId' "$MACHINE_KEY_FILE")
USER_ID=$(jq -r '.userId' "$MACHINE_KEY_FILE")

NOW=$(date +%s)
HEADER=$(printf '{"alg":"RS256","kid":"%s"}' "$KEY_ID" | base64url)
PAYLOAD=$(printf '{"iss":"%s","sub":"%s","aud":"%s","iat":%d,"exp":%d}' \
  "$USER_ID" "$USER_ID" "$ZITADEL_URL" "$NOW" "$((NOW + 3600))" | base64url)
SIGNATURE=$(printf '%s.%s' "$HEADER" "$PAYLOAD" | \
  openssl dgst -sha256 -sign <(jq -r '.key' "$MACHINE_KEY_FILE") | base64url)
JWT="${HEADER}.${PAYLOAD}.${SIGNATURE}"

echo "Authenticating with service account..."
TOKEN_RESPONSE=$(curl -s "$ZITADEL_URL/oauth/v2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" \
  -d "scope=openid urn:zitadel:iam:org:project:id:zitadel:aud" \
  -d "assertion=$JWT")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')

if [ -z "$ACCESS_TOKEN" ]; then
  echo "ERROR: Could not obtain access token."
  echo "Response: $TOKEN_RESPONSE"
  echo "Falling back to manual setup via Zitadel console at $ZITADEL_URL/ui/console"
  echo "Admin credentials: admin / Password1!"
  exit 1
fi

AUTH_HEADER="Authorization: Bearer ${ACCESS_TOKEN}"

echo "Creating project: ${PROJECT_NAME}..."
PROJECT_RESPONSE=$(curl -s -X POST "$ZITADEL_URL/management/v1/projects" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"${PROJECT_NAME}\", \"projectRoleAssertion\": true}")

PROJECT_ID=$(echo "$PROJECT_RESPONSE" | jq -r '.id // empty')

if [ -z "$PROJECT_ID" ]; then
  echo "ERROR: Failed to create project."
  echo "Response: $PROJECT_RESPONSE"
  exit 1
fi

echo "Project created: ${PROJECT_ID}"

echo "Creating project roles..."
ROLES_RESPONSE=$(curl -s -X POST "$ZITADEL_URL/management/v1/projects/${PROJECT_ID}/roles/_bulk" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{"roles": [
    {"key": "custody:read", "displayName": "Custody Read"},
    {"key": "custody:write", "displayName": "Custody Write"}
  ]}')
echo "Roles created"

echo "Creating machine-to-machine application..."
APP_RESPONSE=$(curl -s -X POST "$ZITADEL_URL/management/v1/projects/${PROJECT_ID}/apps/api" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{"name": "custody-service", "authMethodType": "API_AUTH_METHOD_TYPE_BASIC"}')

CLIENT_ID=$(echo "$APP_RESPONSE" | jq -r '.clientId // empty')
CLIENT_SECRET=$(echo "$APP_RESPONSE" | jq -r '.clientSecret // empty')

if [ -z "$CLIENT_ID" ]; then
  echo "ERROR: Failed to create application."
  echo "Response: $APP_RESPONSE"
  exit 1
fi

echo ""
echo "========================================="
echo "  Zitadel OAuth Setup Complete"
echo "========================================="
echo "  Console:       ${ZITADEL_URL}/ui/console"
echo "  Admin user:    admin / Password1!"
echo "  Project:       ${PROJECT_NAME} (${PROJECT_ID})"
echo "  Client ID:     ${CLIENT_ID}"
echo "  Client Secret: ${CLIENT_SECRET}"
echo "  Token URL:     ${ZITADEL_URL}/oauth/v2/token"
echo "  Issuer:        ${ZITADEL_URL}"
echo "  Roles:         custody:read, custody:write"
echo "========================================="
