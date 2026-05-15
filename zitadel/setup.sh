#!/bin/bash
set -euo pipefail

ZITADEL_URL="http://localhost:8085"
ADMIN_USER="admin"
ADMIN_PASSWORD="Password1!"
PROJECT_NAME="custody-fireblocks"

echo "Waiting for Zitadel to be ready..."
until curl -sf "$ZITADEL_URL/debug/ready" > /dev/null 2>&1; do
  sleep 2
done
echo "Zitadel is ready"

echo "Authenticating as admin..."
TOKEN_RESPONSE=$(curl -s "$ZITADEL_URL/oauth/v2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=zitadel-admin-sa" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "scope=openid urn:zitadel:iam:org:project:id:zitadel:aud")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Failed to authenticate. Trying PAT approach..."
  PAT_RESPONSE=$(curl -s -X POST "$ZITADEL_URL/management/v1/users/me/pats" \
    -u "${ADMIN_USER}:${ADMIN_PASSWORD}" \
    -H "Content-Type: application/json" \
    -d '{"expirationDate": "2030-01-01T00:00:00Z"}')
  ACCESS_TOKEN=$(echo "$PAT_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
fi

if [ -z "$ACCESS_TOKEN" ]; then
  echo "ERROR: Could not obtain access token. Please create the OAuth application manually via Zitadel console at $ZITADEL_URL/ui/console"
  echo "Admin credentials: ${ADMIN_USER} / ${ADMIN_PASSWORD}"
  exit 1
fi

AUTH_HEADER="Authorization: Bearer ${ACCESS_TOKEN}"

echo "Creating project: ${PROJECT_NAME}..."
PROJECT_RESPONSE=$(curl -s -X POST "$ZITADEL_URL/management/v1/projects" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"${PROJECT_NAME}\", \"projectRoleAssertion\": true}")

PROJECT_ID=$(echo "$PROJECT_RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Project created: ${PROJECT_ID}"

echo "Creating project roles..."
curl -s -X POST "$ZITADEL_URL/management/v1/projects/${PROJECT_ID}/roles/_bulk" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{"roles": [
    {"key": "custody:read", "displayName": "Custody Read"},
    {"key": "custody:write", "displayName": "Custody Write"}
  ]}' > /dev/null

echo "Creating machine-to-machine application..."
APP_RESPONSE=$(curl -s -X POST "$ZITADEL_URL/management/v1/projects/${PROJECT_ID}/apps/api" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{"name": "custody-service", "authMethodType": "API_AUTH_METHOD_TYPE_BASIC"}')

CLIENT_ID=$(echo "$APP_RESPONSE" | grep -o '"clientId":"[^"]*"' | cut -d'"' -f4)
CLIENT_SECRET=$(echo "$APP_RESPONSE" | grep -o '"clientSecret":"[^"]*"' | cut -d'"' -f4)

echo ""
echo "========================================="
echo "  Zitadel OAuth Setup Complete"
echo "========================================="
echo "  Console:       ${ZITADEL_URL}/ui/console"
echo "  Admin user:    ${ADMIN_USER} / ${ADMIN_PASSWORD}"
echo "  Project:       ${PROJECT_NAME} (${PROJECT_ID})"
echo "  Client ID:     ${CLIENT_ID}"
echo "  Client Secret: ${CLIENT_SECRET}"
echo "  Token URL:     ${ZITADEL_URL}/oauth/v2/token"
echo "  Issuer:        ${ZITADEL_URL}"
echo "  Roles:         custody:read, custody:write"
echo "========================================="
