#!/bin/bash

# JWT Authentication Verification Script
# This script runs all the JWT authentication verification steps

# Set default values
USERNAME="admin"
PASSWORD="datalink"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -u|--username)
      USERNAME="$2"
      shift 2
      ;;
    -p|--password)
      PASSWORD="$2"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [options]"
      echo ""
      echo "Options:"
      echo "  -u, --username USERNAME  Specify the username (default: admin)"
      echo "  -p, --password PASSWORD  Specify the password (default: datalink)"
      echo "  -h, --help               Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use --help for usage information"
      exit 1
      ;;
  esac
done

echo "=== JWT Authentication Verification ==="
echo "Username: $USERNAME"
echo "Password: ${PASSWORD//?/*}"
echo ""

# Step 1: Extract JWT configuration from backend code
echo "Step 1: Extracting JWT configuration from backend code..."
node scripts/extract-jwt-config.js
echo ""

# Step 2: Inspect backend token
echo "Step 2: Inspecting backend token..."
node scripts/inspect-backend-token.js "$USERNAME" "$PASSWORD"
echo ""

# Step 3: Verify JWT signature
echo "Step 3: Verifying JWT signature..."
node scripts/verify-jwt-signature.js "$USERNAME"
echo ""

# Step 4: Generate JWT token
echo "Step 4: Generating JWT token..."
node scripts/generate-jwt-token.js "$USERNAME"
echo ""

# Step 5: Test JWT authentication
echo "Step 5: Testing JWT authentication..."
node scripts/test-jwt.js "$USERNAME" "$PASSWORD"
echo ""

echo "=== Verification Complete ==="
echo "If all steps completed successfully, JWT authentication is working correctly."
echo "If any step failed, check the error messages for details."
