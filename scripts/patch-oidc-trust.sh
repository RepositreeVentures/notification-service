#!/usr/bin/env bash
# Patches the repositree-codeartifact-consume IAM role trust policy to allow
# GitHub Actions OIDC from this repository.
#
# Run this once from a terminal with iam:GetRole + iam:UpdateAssumeRolePolicy.

set -euo pipefail

ROLE_NAME="repositree-codeartifact-consume"
REPO="RepositreeVentures/notification-service"
OIDC_PROVIDER="token.actions.githubusercontent.com"

echo "Fetching current trust policy for $ROLE_NAME..."
CURRENT=$(aws iam get-role --role-name "$ROLE_NAME" \
  --query 'Role.AssumeRolePolicyDocument' --output json)

echo "Current policy:"
echo "$CURRENT" | jq .

# Check if this repo is already in the trust policy
if echo "$CURRENT" | grep -q "$REPO"; then
  echo "Repo $REPO is already trusted. Nothing to do."
  exit 0
fi

# Build the new statement to add
NEW_STATEMENT=$(cat <<EOF
{
  "Effect": "Allow",
  "Principal": {
    "Federated": "arn:aws:iam::955413563895:oidc-provider/$OIDC_PROVIDER"
  },
  "Action": "sts:AssumeRoleWithWebIdentity",
  "Condition": {
    "StringEquals": {
      "$OIDC_PROVIDER:aud": "sts.amazonaws.com"
    },
    "StringLike": {
      "$OIDC_PROVIDER:sub": "repo:$REPO:*"
    }
  }
}
EOF
)

# Merge the new statement into the existing policy
UPDATED=$(echo "$CURRENT" | jq --argjson stmt "$NEW_STATEMENT" \
  '.Statement += [$stmt]')

echo ""
echo "Updated policy:"
echo "$UPDATED" | jq .

read -rp "Apply this change? [y/N] " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
  echo "Aborted."
  exit 1
fi

aws iam update-assume-role-policy \
  --role-name "$ROLE_NAME" \
  --policy-document "$UPDATED"

echo "Done. CI should now be able to assume $ROLE_NAME."
