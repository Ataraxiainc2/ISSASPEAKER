#!/usr/bin/env bash
# Sets Google's trust rule for GitHub -> Google Play (Workload Identity provider "issaspeaker-repo").
#   bash tools/play/trust-rule.sh <commit SHA> [<commit SHA> ...]
# Google issues a Play token only to a job that runs play-access.yml or play-upload.yml of ISSASPEAKER at one
# of the listed commits, in a repository of owner id 79027743, in the environment "play", on main or master.
# After changing either workflow: push, run this with the new commit AND the old one, move the callers to the
# new commit, then run it again with the new commit only. Owner-only: needs the owner's gcloud login.
set -euo pipefail
[ $# -ge 1 ] || { echo "usage: $0 <commit SHA> [...]" >&2; exit 2; }
shas=""
for sha in "$@"; do
  [[ "$sha" =~ ^[0-9a-f]{40}$ ]] || { echo "not a full commit SHA: $sha" >&2; exit 2; }
  shas="${shas:+$shas, }'$sha'"
done
wf="Ataraxiainc2/ISSASPEAKER/.github/workflows"
condition="assertion.repository_owner_id == '79027743'"
condition+=" && assertion.environment == 'play'"
condition+=" && assertion.ref in ['refs/heads/main', 'refs/heads/master']"
condition+=" && (assertion.job_workflow_ref.startsWith('$wf/play-access.yml@')"
condition+=" || assertion.job_workflow_ref.startsWith('$wf/play-upload.yml@'))"
condition+=" && assertion.job_workflow_sha in [$shas]"
gcloud iam workload-identity-pools providers update-oidc issaspeaker-repo \
  --project=issaspeaker --location=global --workload-identity-pool=github \
  --account=sat.tecnologias@gmail.com --attribute-condition="$condition" \
  --format='value(attributeCondition)'
