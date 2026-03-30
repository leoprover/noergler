Nörgler TSTP proof checker

```
usage: noergler [options] <problem file> <proof file>

 Nörgler is a proof checker for proofs from automated theorem provers
 in the TSTP format from the TPTP infrastructure. Currently, only
 checking of refutational FOF proofs is supported.
 The TSTP proof of <problem file> is supplied in <proof file>.

 Nörgler will check the following things:
   - Uniqueness of formula names
   - Conclusion of proof with $false
   - Acyclicity of inference parent graph (from $false upward)
   - Existence of inference parents in each proof step earlier in proof
   - Correctness of copies of axioms/conjecture from problem file
   - Correctness of negation of conjecture
   - Correctness of Skolemization steps wrt. simple internal skolemization procedure
   - Provability of thm/cth steps in proof using external provers

 If one of these steps fail with an error, Nörgler will return SZS status
 FailedVerified.
 If one of these steps time out (most likely the check of provability of thm/cth steps)
 SZS status NotVerified is returned.
 The former SZS status claims that the proof is incorrect, while the latter status
 does not make any claims with regard to correctness.

 Options:
  --timeout t  Timeout after n seconds (soft limit, best effort).
               A timeout will result in a SZS status NotVerified output.

  --parallel   If set, Nörgler will make use of threaded parellelism, potentially
               on different CPU cores if available.

  --verbosity n
               Set the verbosity of logging to std.err. If n = 0, logging is disabled;
               n = 6 is maximal verbosity (very fine-grained logging output).

  --version    Print the version number of the executable and terminate.

  --help       Print this description and terminate.
```
