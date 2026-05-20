#!/bin/bash
### Simple script for executing Nörgler in the ProoVer competition
### Alexander Steen, 2026

# Resolve the full path of the script
HERE=$(dirname "$(readlink -f "$0")")
# Resolve the full file path of noergler
NOERGLER=$(readlink -f "$1")
# Resolve the full file path of the proof file
PROOF=$(readlink -f "$2")
# Path of proof file
PROOFPATH=$(dirname "$PROOF")

#echo "HERE: $HERE"
#echo "PROOF: $PROOF"
#echo "PROOFPATH: $PROOFPATH"
#echo "NOERGLER: $NOERGLER"
# Get "Problem: " annotation from proof file
PROBLEMREL=$(cat "$PROOF" | grep "% Proof" | cut -d ":" -f 2 | tr -d ' ')
#echo "RELATIVE PATH OF PROBLEM: $PROBLEMREL"
PROBLEM="$PROOFPATH/$PROBLEMREL"
#echo "ABSOLUTE PATH OF PROBLEM: $PROBLEM"

## Run
$NOERGLER --problem "$PROBLEM" "$PROOF"
