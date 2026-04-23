
%------------------------------------------------------------------------------
% File     : SET883+1 : TPTP v9.2.1. Released v3.2.0.
% Domain   : Set theory
% Problem  : subset(singleton(A),singleton(B)) => A = B
% Version  : [Urb06] axioms : Especial.
% English  :

% Refs     : [Byl90] Bylinski (1990), Some Basic Properties of Sets
%          : [Urb06] Urban (2006), Email to G. Sutcliffe
% Source   : [Urb06]
% Names    : zfmisc_1__t24_zfmisc_1 [Urb06]

% Status   : Theorem
% Rating   : 0.03 v9.1.0, 0.00 v6.4.0, 0.04 v6.1.0, 0.07 v6.0.0, 0.09 v5.5.0, 0.04 v5.3.0, 0.11 v5.2.0, 0.05 v5.0.0, 0.04 v3.7.0, 0.05 v3.3.0, 0.00 v3.2.0
% Syntax   : Number of formulae    :    5 (   3 unt;   0 def)
%            Number of atoms       :    7 (   2 equ)
%            Maximal formula atoms :    2 (   1 avg)
%            Number of connectives :    3 (   1   ~;   0   |;   0   &)
%                                         (   0 <=>;   2  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    4 (   3 avg)
%            Maximal term depth    :    2 (   1 avg)
%            Number of predicates  :    3 (   2 usr;   0 prp; 1-2 aty)
%            Number of functors    :    1 (   1 usr;   0 con; 1-1 aty)
%            Number of variables   :    8 (   6   !;   2   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : Translated by MPTP 0.2 from the original problem in the Mizar
%            library, www.mizar.org
%------------------------------------------------------------------------------
fof(reflexivity_r1_tarski,axiom,
    ! [A,B] : subset(A,A) ).

fof(rc1_xboole_0,axiom,
    ? [A] : empty(A) ).

fof(rc2_xboole_0,axiom,
    ? [A] : ~ empty(A) ).

fof(t24_zfmisc_1,conjecture,
    ! [A,B] :
      ( subset(singleton(A),singleton(B))
     => A = B ) ).

fof(t6_zfmisc_1,axiom,
    ! [A,B] :
      ( subset(singleton(A),singleton(B))
     => A = B ) ).

%------------------------------------------------------------------------------
