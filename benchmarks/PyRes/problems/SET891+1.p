
%------------------------------------------------------------------------------
% File     : SET891+1 : TPTP v9.2.1. Released v3.2.0.
% Domain   : Set theory
% Problem  : union(uno_pair(singleton(A),singleton(B))) = uno_pair(A,B)
% Version  : [Urb06] axioms : Especial.
% English  :

% Refs     : [Byl90] Bylinski (1990), Some Basic Properties of Sets
%          : [Urb06] Urban (2006), Email to G. Sutcliffe
% Source   : [Urb06]
% Names    : zfmisc_1__t32_zfmisc_1 [Urb06]

% Status   : Theorem
% Rating   : 0.06 v9.1.0, 0.03 v9.0.0, 0.06 v8.1.0, 0.03 v7.1.0, 0.04 v7.0.0, 0.07 v6.4.0, 0.12 v6.3.0, 0.08 v6.1.0, 0.20 v6.0.0, 0.13 v5.5.0, 0.07 v5.4.0, 0.11 v5.3.0, 0.19 v5.2.0, 0.05 v5.0.0, 0.12 v4.1.0, 0.17 v4.0.1, 0.22 v4.0.0, 0.21 v3.7.0, 0.10 v3.5.0, 0.11 v3.3.0, 0.07 v3.2.0
% Syntax   : Number of formulae    :   10 (   8 unt;   0 def)
%            Number of atoms       :   12 (   6 equ)
%            Maximal formula atoms :    2 (   1 avg)
%            Number of connectives :    7 (   5   ~;   0   |;   0   &)
%                                         (   0 <=>;   2  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    5 (   3 avg)
%            Maximal term depth    :    4 (   1 avg)
%            Number of predicates  :    2 (   1 usr;   0 prp; 1-2 aty)
%            Number of functors    :    4 (   4 usr;   0 con; 1-2 aty)
%            Number of variables   :   18 (  16   !;   2   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : Translated by MPTP 0.2 from the original problem in the Mizar
%            library, www.mizar.org
%------------------------------------------------------------------------------
fof(commutativity_k2_tarski,axiom,
    ! [A,B] : unordered_pair(A,B) = unordered_pair(B,A) ).

fof(commutativity_k2_xboole_0,axiom,
    ! [A,B] : set_union2(A,B) = set_union2(B,A) ).

fof(fc2_xboole_0,axiom,
    ! [A,B] :
      ( ~ empty(A)
     => ~ empty(set_union2(A,B)) ) ).

fof(fc3_xboole_0,axiom,
    ! [A,B] :
      ( ~ empty(A)
     => ~ empty(set_union2(B,A)) ) ).

fof(idempotence_k2_xboole_0,axiom,
    ! [A,B] : set_union2(A,A) = A ).

fof(l52_zfmisc_1,axiom,
    ! [A,B] : union(unordered_pair(A,B)) = set_union2(A,B) ).

fof(rc1_xboole_0,axiom,
    ? [A] : empty(A) ).

fof(rc2_xboole_0,axiom,
    ? [A] : ~ empty(A) ).

fof(t32_zfmisc_1,conjecture,
    ! [A,B] : union(unordered_pair(singleton(A),singleton(B))) = unordered_pair(A,B) ).

fof(t41_enumset1,axiom,
    ! [A,B] : unordered_pair(A,B) = set_union2(singleton(A),singleton(B)) ).

%------------------------------------------------------------------------------
