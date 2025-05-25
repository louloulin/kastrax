#!/bin/bash

# JWT Authentication Test Script
# This script runs the JWT authentication test

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

echo "=== JWT Authentication Test ==="
echo "Username: $USERNAME"
echo "Password: ${PASSWORD//?/*}"
echo ""

# Run the test
node scripts/test-jwt.js "$USERNAME" "$PASSWORD"

# Check the exit code
if [ $? -eq 0 ]; then
  echo ""
  echo "=== Test Completed Successfully ==="
  echo "JWT authentication is working correctly."
else
  echo ""
  echo "=== Test Failed ==="
  echo "JWT authentication is not working correctly."
  echo "Please check the error messages above for details."
  exit 1
fi
