#!/bin/bash
### Simple script for executing Nörgler in the ProoVer competition
### Alexander Steen, 2026

if [[ $# -eq 0 ]] ; then
    echo 'usage: proover_run.sh <proof path> <timeout>'
    exit 0
fi

# Resolve the full path of the script
HERE=$(dirname "$(readlink -f "$0")")
# Resolve the full file path of noergler // TODO: Adapt to proper path when needed, e.g., relative to $HERE
NOERGLER=$(which "noergler")
# Resolve the full file path of eprover // TODO: Adapt to proper path when needed, e.g., relative to $HERE
EPROVER=$(which "eprover")
# Resolve the full file path of mace4 // TODO: Adapt to proper path when needed, e.g., relative to $HERE
MACE4=$(which "mace4")
# Resolve the full file path of the proof file
PROOF=$(readlink -f "$1")
# Path of proof file
PROOFPATH=$(dirname "$PROOF")
# Timeout
TIMEOUT="$2"

# Get "Problem: " annotation from proof file
PROBLEMREL=$(cat "$PROOF" | grep "% Proof" | cut -d ":" -f 2 | tr -d ' ')
PROBLEM="$PROOFPATH/$PROBLEMREL"

## Run
RESULT=$(timeout --kill-after=0.5s "$TIMEOUT" $NOERGLER --problem "$PROBLEM" --verbosity 1 --mace4-path "$MACE4" --eprover-path "$EPROVER" --parallel-mode steps --parallel-model-finder-mode offset --find-failing-step "$PROOF") 
EXITCODE=$?

if [[ $EXITCODE -eq 124 ]]; then
    echo "% SZS status Timeout"
else
    echo "$RESULT"
fi

