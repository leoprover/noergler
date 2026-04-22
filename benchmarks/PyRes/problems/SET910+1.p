
%------------------------------------------------------------------------------
% File     : SET910+1 : TPTP v9.2.1. Released v3.2.0.
% Domain   : Set theory
% Problem  : intersection(A,singleton(B)) = singleton(B) => in(B,A)
% Version  : [Urb06] axioms : Especial.
% English  :

% Refs     : [Byl90] Bylinski (1990), Some Basic Properties of Sets
%          : [Urb06] Urban (2006), Email to G. Sutcliffe
% Source   : [Urb06]
% Names    : zfmisc_1__t51_zfmisc_1 [Urb06]

% Status   : Theorem
% Rating   : 0.03 v9.1.0, 0.00 v6.4.0, 0.04 v6.2.0, 0.08 v6.1.0, 0.13 v5.5.0, 0.07 v5.3.0, 0.15 v5.2.0, 0.05 v5.0.0, 0.04 v3.7.0, 0.05 v3.4.0, 0.11 v3.3.0, 0.07 v3.2.0
% Syntax   : Number of formulae    :    7 (   4 unt;   0 def)
%            Number of atoms       :   10 (   4 equ)
%            Maximal formula atoms :    2 (   1 avg)
%            Number of connectives :    5 (   2   ~;   0   |;   0   &)
%                                         (   0 <=>;   3  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    5 (   3 avg)
%            Maximal term depth    :    3 (   1 avg)
%            Number of predicates  :    3 (   2 usr;   0 prp; 1-2 aty)
%            Number of functors    :    2 (   2 usr;   0 con; 1-2 aty)
%            Number of variables   :   12 (  10   !;   2   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : Translated by MPTP 0.2 from the original problem in the Mizar
%            library, www.mizar.org
%------------------------------------------------------------------------------
fof(commutativity_k3_xboole_0,axiom,
    ! [A,B] : set_intersection2(A,B) = set_intersection2(B,A) ).

fof(idempotence_k3_xboole_0,axiom,
    ! [A,B] : set_intersection2(A,A) = A ).

fof(antisymmetry_r2_hidden,axiom,
    ! [A,B] :
      ( in(A,B)
     => ~ in(B,A) ) ).

fof(rc1_xboole_0,axiom,
    ? [A] : empty(A) ).

fof(rc2_xboole_0,axiom,
    ? [A] : ~ empty(A) ).

fof(t51_zfmisc_1,conjecture,
    ! [A,B] :
      ( set_intersection2(A,singleton(B)) = singleton(B)
     => in(B,A) ) ).

fof(l30_zfmisc_1,axiom,
    ! [A,B] :
      ( set_intersection2(A,singleton(B)) = singleton(B)
     => in(B,A) ) ).

%------------------------------------------------------------------------------
