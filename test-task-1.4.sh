#!/bin/bash

# Task 1.4 Testing Script
# This script tests the Repository CRUD endpoints

BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "Task 1.4: Repository CRUD Testing"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Create a test user directly in the database
echo -e "${YELLOW}Step 1: Creating test user in database...${NC}"
docker exec -it codebaseqa-postgres psql -U postgres -d codebaseqa -c "
INSERT INTO users (id, github_id, username, email, avatar_url, github_token, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440000',
  12345678,
  'testuser',
  'test@example.com',
  'https://avatars.githubusercontent.com/u/12345678',
  'ghp_test_token_replace_with_real_token',
  NOW(),
  NOW()
)
ON CONFLICT (id) DO NOTHING;
" 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Test user created${NC}"
else
    echo -e "${RED}✗ Failed to create test user${NC}"
fi
echo ""

# Step 2: Generate JWT for test user
echo -e "${YELLOW}Step 2: Generating JWT token...${NC}"
echo -e "${YELLOW}Note: You need to use the /api/auth/github flow or manually generate a JWT${NC}"
echo -e "${YELLOW}For now, we'll test without authentication (will get 401/403)${NC}"
echo ""

# Step 3: Test endpoints (will fail without proper JWT)
echo -e "${YELLOW}Step 3: Testing Repository Endpoints${NC}"
echo ""

# Test 1: List repositories (should return 401 without JWT)
echo "Test 1: GET /api/repos (List repositories)"
echo "Expected: 401 Unauthorized (no JWT provided)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X GET "$BASE_URL/repos" \
  -H "Content-Type: application/json"
echo ""
echo ""

# Test 2: Connect repository (should return 401 without JWT)
echo "Test 2: POST /api/repos (Connect repository)"
echo "Expected: 401 Unauthorized (no JWT provided)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/repos" \
  -H "Content-Type: application/json" \
  -d '{
    "repoFullName": "octocat/Hello-World",
    "branch": "main"
  }'
echo ""
echo ""

# Test 3: Invalid repo format
echo "Test 3: POST /api/repos (Invalid repo format)"
echo "Expected: 400 Bad Request (validation error)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/repos" \
  -H "Content-Type: application/json" \
  -d '{
    "repoFullName": "invalid-format",
    "branch": "main"
  }'
echo ""
echo ""

echo "=========================================="
echo "Testing Complete"
echo "=========================================="
echo ""
echo -e "${YELLOW}To test with authentication:${NC}"
echo "1. Set up GitHub OAuth app (see 08-configuration.md)"
echo "2. Update application.yml with OAuth credentials"
echo "3. Visit http://localhost:8080/api/auth/github"
echo "4. Complete OAuth flow to get JWT token"
echo "5. Use the JWT token in Authorization header:"
echo "   curl -H 'Authorization: Bearer YOUR_JWT_TOKEN' http://localhost:8080/api/repos"
echo ""
