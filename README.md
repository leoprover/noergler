# Nörgler TSTP proof checker

Nörgler may be cited as [![DOI](https://zenodo.org/badge/1193577802.svg)](https://doi.org/10.5281/zenodo.19787117).


```
usage: noergler [options] [--problem <problem file>] <proof file>

 Nörgler is a proof checker for proofs from automated theorem provers
 in the TSTP format from the TPTP infrastructure. Checking of refutations
 of FOF, TFF and THF formulae is supported.

 The proof is supplied in <proof file>. If a <problem file> is also provided,
 Nörgler will verify the proof relative to the problem's axioms and conjecture.

 Nörgler will always check the following structural and logical properties:
   - Uniqueness of formula names
   - Conclusion of proof with $false
   - Acyclicity of inference parent graph (from $false upward)
   - Existence of inference parents in each proof step earlier in proof
   - Correctness of Skolemization steps wrt. simple internal skolemization procedure
   - Provability of thm/cth steps in proof using external provers
   - Conjecture is never directly used in refutation (only as negated conjecture)
   - Only the conjecture is negated using a cth status inference.

 If a <problem file> is provided, Nörgler additionally checks:
   - Correctness of copies of axioms/conjecture from problem file
   - Correctness of negation of conjecture

 If one of these steps fail with an error, Nörgler will return SZS status
 VerifiedBad.
 If one of these steps time out (most likely the check of provability of thm/cth steps)
 SZS status Timout is returned, other non-success reults are returned as SZS status Unknown.
 The former SZS status claims that the proof is incorrect, while the latter status
 does not make any claims with regard to correctness.

 Options:
  --timeout t      Timeout after n seconds (soft limit, best effort).
                   A timeout will result in a SZS status NotVerified output.

  --prover name(s) Select the prover(s) to use for verifying thm/cth steps.
                   Options: 'eprover', 'vampire', 'all' (use all available),
                   or a comma-separated list (e.g., 'eprover,vampire').
                   If multiple provers are given and no parallelization
                   strategy running multiple provers at once is given,
                   the provers are tried in the sequence they are provided in.
                   Otherwise, they are run in parallel.
                   Default:'eprover'

  --eprover-path path
                  Path to eprover, if not findable by `which eprover`.

  --vampire-path path
                  Path to vampire, if not findable by `which vampire`.

  --mace4-path path
                  Path to mace4, if not findable by `which mace4`.

  --model-finder  Select the model finder to be used.
                  Options:'mace4'
                  Default:'mace4'

  --parallel-mode mode Set the parallelization strategy.
                  Options:
                     - none: Sequential execution (default).
                     - steps: Verify different proof steps in parallel.
                     - provers: Run multiple provers on the same step in parallel.
                     - hybrid: Parallelize both steps and provers.

  --parallel-model-finder-mode
                  Options:
                     - none: Do not apply model finders (default)
                     - fallback: Countermodel finder only used as a fallback after
                       all chosen provers failed to find a solution
                     - always: Always also run counter model finder in parallel to provers
                       when running generic inference checks.
                     - offset: start countermodel-finder in parallel after running
                       provers without success for 1 second.

  --verbosity n   Set the verbosity of logging to std.err. If n = 0, logging is disabled;
                  n = 6 is maximal verbosity (very fine-grained logging output).

  --version       Print the version number of the executable and terminate.

  --help          Print this description and terminate.

  --up-to-esa     If set, Nörgler will not attempt to verify ESA steps (including Skolemization).
                  Recommended for provers that do not follow to precise Skolemization annotation format.

  --relax-annotation-format
                  If set, Nörgler will be more permissive about the format of the formula annotation
                  as long as parent-child relationships can still be inferred. In particular, Nörgler will
                  i) allow nested application of inferences in annotations
                  ii) allow negation of the conjecture with inference names other than negated_conjecture, as
                      long as the status of the inference is cth, and the step has role "negated_conjecture"

  --relax-problem-check
                  If set, Nörgler will be more permissive about the relationship between formulas from problem
                  files and their copies in the given proofs. In particular, Nörgler will
                  i) allow formulas with different roles

  --relax-specified-inference-checks
                  Effects those steps derived by inferences with specified inference rules that trigger
                  checks via comparison to internally derived results (currently negated_conjecture
                  and Skolemization). If set, Nörgler will test if the step of the proof is derivable from the
                  internally produced version, as a fallback if formulas are not identical.

  --allow-prover-axioms
                  If set, Nörgler will allow axioms with an annotation other than file, but print a warning
                  indicating that an axiom was introduced. Recommended for provers that introduce built-in
                  theory axioms.
```
