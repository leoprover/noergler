
%------------------------------------------------------------------------------
% File     : SET978+1 : TPTP v9.2.1. Released v3.2.0.
% Domain   : Set theory
% Problem  : Basic properties of sets, theorem 131
% Version  : [Urb06] axioms : Especial.
% English  :

% Refs     : [Byl90] Bylinski (1990), Some Basic Properties of Sets
%          : [Urb06] Urban (2006), Email to G. Sutcliffe
% Source   : [Urb06]
% Names    : zfmisc_1__t131_zfmisc_1 [Urb06]

% Status   : Theorem
% Rating   : 0.06 v9.1.0, 0.03 v7.2.0, 0.00 v6.4.0, 0.04 v6.3.0, 0.08 v6.1.0, 0.07 v6.0.0, 0.04 v5.5.0, 0.00 v5.4.0, 0.07 v5.3.0, 0.11 v5.2.0, 0.00 v5.0.0, 0.04 v3.7.0, 0.05 v3.4.0, 0.00 v3.2.0
% Syntax   : Number of formulae    :    6 (   2 unt;   0 def)
%            Number of atoms       :   12 (   2 equ)
%            Maximal formula atoms :    3 (   2 avg)
%            Number of connectives :    9 (   3   ~;   1   |;   1   &)
%                                         (   0 <=>;   4  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    7 (   5 avg)
%            Maximal term depth    :    3 (   1 avg)
%            Number of predicates  :    3 (   2 usr;   0 prp; 1-2 aty)
%            Number of functors    :    2 (   2 usr;   0 con; 1-2 aty)
%            Number of variables   :   14 (  12   !;   2   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : Translated by MPTP 0.2 from the original problem in the Mizar
%            library, www.mizar.org
%------------------------------------------------------------------------------
fof(rc1_xboole_0,axiom,
    ? [A] : empty(A) ).

fof(rc2_xboole_0,axiom,
    ? [A] : ~ empty(A) ).

fof(symmetry_r1_xboole_0,axiom,
    ! [A,B] :
      ( disjoint(A,B)
     => disjoint(B,A) ) ).

fof(t127_zfmisc_1,axiom,
    ! [A,B,C,D] :
      ( ( disjoint(A,B)
        | disjoint(C,D) )
     => disjoint(cartesian_product2(A,C),cartesian_product2(B,D)) ) ).

fof(t131_zfmisc_1,conjecture,
    ! [A,B,C,D] :
      ( A != B
     => ( disjoint(cartesian_product2(singleton(A),C),cartesian_product2(singleton(B),D))
        & disjoint(cartesian_product2(C,singleton(A)),cartesian_product2(D,singleton(B))) ) ) ).

fof(t17_zfmisc_1,axiom,
    ! [A,B] :
      ( A != B
     => disjoint(singleton(A),singleton(B)) ) ).

%------------------------------------------------------------------------------
