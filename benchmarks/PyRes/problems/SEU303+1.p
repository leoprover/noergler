
%------------------------------------------------------------------------------
% File     : SEU303+1 : TPTP v9.2.1. Released v3.3.0.
% Domain   : Set theory
% Problem  : MPTP bushy problem t26_finset_1
% Version  : [Urb07] axioms : Especial.
% English  :

% Refs     : [Ban01] Bancerek et al. (2001), On the Characterizations of Co
%          : [Urb07] Urban (2006), Email to G. Sutcliffe
% Source   : [Urb07]
% Names    : bushy-t26_finset_1 [Urb07]

% Status   : Theorem
% Rating   : 0.03 v9.1.0, 0.00 v9.0.0, 0.03 v7.1.0, 0.04 v7.0.0, 0.03 v6.4.0, 0.08 v6.3.0, 0.04 v6.1.0, 0.10 v6.0.0, 0.04 v5.4.0, 0.07 v5.3.0, 0.11 v5.2.0, 0.00 v4.0.0, 0.04 v3.7.0, 0.05 v3.3.0
% Syntax   : Number of formulae    :    8 (   3 unt;   0 def)
%            Number of atoms       :   19 (   1 equ)
%            Maximal formula atoms :    4 (   2 avg)
%            Number of connectives :   11 (   0   ~;   0   |;   5   &)
%                                         (   0 <=>;   6  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    6 (   3 avg)
%            Maximal term depth    :    3 (   1 avg)
%            Number of predicates  :    5 (   3 usr;   1 prp; 0-2 aty)
%            Number of functors    :    3 (   3 usr;   0 con; 1-2 aty)
%            Number of variables   :    7 (   6   !;   1   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : Translated by MPTP 0.2 from the original problem in the Mizar
%            library, www.mizar.org
%------------------------------------------------------------------------------
fof(dt_k1_relat_1,axiom,
    $true ).

fof(dt_k2_relat_1,axiom,
    $true ).

fof(dt_k9_relat_1,axiom,
    $true ).

fof(fc13_finset_1,axiom,
    ! [A,B] :
      ( ( relation(A)
        & function(A)
        & finite(B) )
     => finite(relation_image(A,B)) ) ).

fof(rc1_funct_1,axiom,
    ? [A] :
      ( relation(A)
      & function(A) ) ).

fof(t146_relat_1,axiom,
    ! [A] :
      ( relation(A)
     => relation_image(A,relation_dom(A)) = relation_rng(A) ) ).

fof(t17_finset_1,axiom,
    ! [A,B] :
      ( ( relation(B)
        & function(B) )
     => ( finite(A)
       => finite(relation_image(B,A)) ) ) ).

fof(t26_finset_1,conjecture,
    ! [A] :
      ( ( relation(A)
        & function(A) )
     => ( finite(relation_dom(A))
       => finite(relation_rng(A)) ) ) ).

%------------------------------------------------------------------------------
