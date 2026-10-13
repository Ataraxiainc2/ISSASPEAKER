#!/usr/bin/env bash
# Gives a repository of Ataraxiainc2 access to Google Play through play-access.yml (keyless, WIF).
#   bash tools/play/enable-repo.sh <repository-name>
# Creates the "play" environment limited to the default branch. Public repositories also get the owner as a
# required reviewer; GitHub only offers required reviewers on private repositories with a paid plan.
# Google Cloud needs no change: its trust rule already covers every repository of owner id 79027743.
set -euo pipefail
repo="Ataraxiainc2/${1:?repository name}"
owner_id=79027743
visibility=$(gh api "repos/$repo" -q .visibility)
if [ "$visibility" = "public" ]; then
  body="{\"reviewers\":[{\"type\":\"User\",\"id\":$owner_id}],\"prevent_self_review\":false,\"deployment_branch_policy\":{\"protected_branches\":false,\"custom_branch_policies\":true}}"
else
  body='{"deployment_branch_policy":{"protected_branches":false,"custom_branch_policies":true}}'
fi
echo "$body" | gh api -X PUT "repos/$repo/environments/play" --input - -q '"environment: \(.name)"'
branch=$(gh api "repos/$repo" -q .default_branch)
gh api -X POST "repos/$repo/environments/play/deployment-branch-policies" -f name="$branch" -f type=branch -q '"branch: \(.name)"' \
  || echo "branch policy for $branch already present"
# Google accepts refs/heads/main and refs/heads/master only; another default branch needs the trust rule updated.
# The repository's play.yml must call play-access.yml at the pinned commit (see docs/design/PLAY-PUBLISHING.md).
