
%------------------------------------------------------------------------------
% File     : SEU219+1 : TPTP v9.2.1. Released v3.3.0.
% Domain   : Set theory
% Problem  : MPTP bushy problem t55_funct_1
% Version  : [Urb07] axioms : Especial.
% English  :

% Refs     : [Ban01] Bancerek et al. (2001), On the Characterizations of Co
%          : [Urb07] Urban (2006), Email to G. Sutcliffe
% Source   : [Urb07]
% Names    : bushy-t55_funct_1 [Urb07]

% Status   : Theorem
% Rating   : 0.12 v9.1.0, 0.03 v9.0.0, 0.06 v8.1.0, 0.03 v7.1.0, 0.04 v7.0.0, 0.07 v6.4.0, 0.12 v6.3.0, 0.04 v6.1.0, 0.10 v6.0.0, 0.00 v5.5.0, 0.04 v5.4.0, 0.11 v5.3.0, 0.15 v5.2.0, 0.00 v4.1.0, 0.04 v4.0.0, 0.08 v3.7.0, 0.05 v3.4.0, 0.11 v3.3.0
% Syntax   : Number of formulae    :   11 (   2 unt;   0 def)
%            Number of atoms       :   32 (   6 equ)
%            Maximal formula atoms :    5 (   2 avg)
%            Number of connectives :   21 (   0   ~;   0   |;  12   &)
%                                         (   0 <=>;   9  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    5 (   3 avg)
%            Maximal term depth    :    3 (   1 avg)
%            Number of predicates  :    5 (   3 usr;   1 prp; 0-2 aty)
%            Number of functors    :    4 (   4 usr;   0 con; 1-1 aty)
%            Number of variables   :    9 (   7   !;   2   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : Translated by MPTP 0.2 from the original problem in the Mizar
%            library, www.mizar.org
%------------------------------------------------------------------------------
fof(d9_funct_1,axiom,
    ! [A] :
      ( ( relation(A)
        & function(A) )
     => ( one_to_one(A)
       => function_inverse(A) = relation_inverse(A) ) ) ).

fof(dt_k1_relat_1,axiom,
    $true ).

fof(dt_k2_funct_1,axiom,
    ! [A] :
      ( ( relation(A)
        & function(A) )
     => ( relation(function_inverse(A))
        & function(function_inverse(A)) ) ) ).

fof(dt_k2_relat_1,axiom,
    $true ).

fof(dt_k4_relat_1,axiom,
    ! [A] :
      ( relation(A)
     => relation(relation_inverse(A)) ) ).

fof(fc3_funct_1,axiom,
    ! [A] :
      ( ( relation(A)
        & function(A)
        & one_to_one(A) )
     => ( relation(relation_inverse(A))
        & function(relation_inverse(A)) ) ) ).

fof(involutiveness_k4_relat_1,axiom,
    ! [A] :
      ( relation(A)
     => relation_inverse(relation_inverse(A)) = A ) ).

fof(rc1_funct_1,axiom,
    ? [A] :
      ( relation(A)
      & function(A) ) ).

fof(rc3_funct_1,axiom,
    ? [A] :
      ( relation(A)
      & function(A)
      & one_to_one(A) ) ).

fof(t37_relat_1,axiom,
    ! [A] :
      ( relation(A)
     => ( relation_rng(A) = relation_dom(relation_inverse(A))
        & relation_dom(A) = relation_rng(relation_inverse(A)) ) ) ).

fof(t55_funct_1,conjecture,
    ! [A] :
      ( ( relation(A)
        & function(A) )
     => ( one_to_one(A)
       => ( relation_rng(A) = relation_dom(function_inverse(A))
          & relation_dom(A) = relation_rng(function_inverse(A)) ) ) ) ).

%------------------------------------------------------------------------------
