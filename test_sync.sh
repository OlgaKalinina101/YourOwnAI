#!/bin/bash

# YourOwnAI Local Sync - Test Script
# Usage: ./test_sync.sh [phone-ip]

PHONE_IP=${1:-"192.168.1.100"}
PORT="8765"
BASE_URL="http://$PHONE_IP:$PORT"

echo "🔍 Testing YourOwnAI Local Sync Server"
echo "   Server: $BASE_URL"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Health Check
echo "1️⃣  Health Check"
echo "   GET /"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "   ${GREEN}✅ SUCCESS${NC} - $BODY"
else
    echo -e "   ${RED}❌ FAILED${NC} - HTTP $HTTP_CODE"
fi
echo ""

# Test 2: Server Status
echo "2️⃣  Server Status"
echo "   GET /status"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/status")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "   ${GREEN}✅ SUCCESS${NC}"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
else
    echo -e "   ${RED}❌ FAILED${NC} - HTTP $HTTP_CODE"
fi
echo ""

# Test 3: Get Conversations
echo "3️⃣  Get Conversations"
echo "   GET /conversations"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/conversations")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    COUNT=$(echo "$BODY" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "?")
    echo -e "   ${GREEN}✅ SUCCESS${NC} - Found $COUNT conversations"
else
    echo -e "   ${RED}❌ FAILED${NC} - HTTP $HTTP_CODE"
fi
echo ""

# Test 4: Get Memories
echo "4️⃣  Get Memories"
echo "   GET /memories"
RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/memories")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    COUNT=$(echo "$BODY" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "?")
    echo -e "   ${GREEN}✅ SUCCESS${NC} - Found $COUNT memories"
else
    echo -e "   ${RED}❌ FAILED${NC} - HTTP $HTTP_CODE"
fi
echo ""

# Test 5: Full Sync
echo "5️⃣  Full Sync"
echo "   POST /sync/full"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sync/full" \
    -H "Content-Type: application/json" \
    -d '{
        "deviceInfo": {
            "deviceId": "test-desktop",
            "deviceName": "Test Desktop",
            "appVersion": "0.1.0",
            "platform": "Desktop"
        },
        "lastSyncTimestamp": 0
    }')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "   ${GREEN}✅ SUCCESS${NC}"
    echo "$BODY" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'   Conversations: {len(data.get(\"conversations\", []))}')
print(f'   Messages: {len(data.get(\"messages\", []))}')
print(f'   Memories: {len(data.get(\"memories\", []))}')
print(f'   Personas: {len(data.get(\"personas\", []))}')
" 2>/dev/null || echo "$BODY"
else
    echo -e "   ${RED}❌ FAILED${NC} - HTTP $HTTP_CODE"
    echo "$BODY"
fi
echo ""

echo "✅ Tests complete!"
