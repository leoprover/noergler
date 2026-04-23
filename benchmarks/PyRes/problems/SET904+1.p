
%------------------------------------------------------------------------------
% File     : SET904+1 : TPTP v9.2.1. Released v3.2.0.
% Domain   : Set theory
% Problem  : subset(set_union2(singleton(A),B),B) => in(A,B)
% Version  : [Urb06] axioms : Especial.
% English  :

% Refs     : [Byl90] Bylinski (1990), Some Basic Properties of Sets
%          : [Urb06] Urban (2006), Email to G. Sutcliffe
% Source   : [Urb06]
% Names    : zfmisc_1__t45_zfmisc_1 [Urb06]

% Status   : Theorem
% Rating   : 0.03 v9.0.0, 0.00 v6.4.0, 0.04 v6.2.0, 0.08 v6.1.0, 0.13 v5.5.0, 0.07 v5.3.0, 0.15 v5.2.0, 0.05 v5.0.0, 0.04 v4.0.1, 0.09 v4.0.0, 0.08 v3.7.0, 0.05 v3.3.0, 0.00 v3.2.0
% Syntax   : Number of formulae    :   10 (   5 unt;   0 def)
%            Number of atoms       :   15 (   2 equ)
%            Maximal formula atoms :    2 (   1 avg)
%            Number of connectives :   11 (   6   ~;   0   |;   0   &)
%                                         (   0 <=>;   5  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    5 (   4 avg)
%            Maximal term depth    :    3 (   1 avg)
%            Number of predicates  :    4 (   3 usr;   0 prp; 1-2 aty)
%            Number of functors    :    2 (   2 usr;   0 con; 1-2 aty)
%            Number of variables   :   18 (  16   !;   2   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : Translated by MPTP 0.2 from the original problem in the Mizar
%            library, www.mizar.org
%------------------------------------------------------------------------------
fof(fc2_xboole_0,axiom,
    ! [A,B] :
      ( ~ empty(A)
     => ~ empty(set_union2(A,B)) ) ).

fof(fc3_xboole_0,axiom,
    ! [A,B] :
      ( ~ empty(A)
     => ~ empty(set_union2(B,A)) ) ).

fof(commutativity_k2_xboole_0,axiom,
    ! [A,B] : set_union2(A,B) = set_union2(B,A) ).

fof(idempotence_k2_xboole_0,axiom,
    ! [A,B] : set_union2(A,A) = A ).

fof(reflexivity_r1_tarski,axiom,
    ! [A,B] : subset(A,A) ).

fof(antisymmetry_r2_hidden,axiom,
    ! [A,B] :
      ( in(A,B)
     => ~ in(B,A) ) ).

fof(rc1_xboole_0,axiom,
    ? [A] : empty(A) ).

fof(rc2_xboole_0,axiom,
    ? [A] : ~ empty(A) ).

fof(t45_zfmisc_1,conjecture,
    ! [A,B] :
      ( subset(set_union2(singleton(A),B),B)
     => in(A,B) ) ).

fof(l21_zfmisc_1,axiom,
    ! [A,B] :
      ( subset(set_union2(singleton(A),B),B)
     => in(A,B) ) ).

%------------------------------------------------------------------------------
