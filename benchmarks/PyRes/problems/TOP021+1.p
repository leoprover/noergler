
%------------------------------------------------------------------------------
% File     : TOP021+1 : TPTP v9.2.1. Released v3.1.0.
% Domain   : Topology
% Problem  : Locally compact tological space
% Version  : [Shu96] axioms : Especial.
% English  :

% Refs     : [Kel55] Kelley (1955), General Topology
%          : [Shu96] Shults (1996), Email to Geoff Sutcliffe
% Source   : [Shu96]
% Names    :

% Status   : Theorem
% Rating   : 0.00 v9.1.0, 0.07 v9.0.0, 0.00 v5.5.0, 0.04 v5.3.0, 0.13 v5.2.0, 0.07 v5.0.0, 0.05 v4.1.0, 0.06 v4.0.1, 0.05 v3.7.0, 0.00 v3.1.0
% Syntax   : Number of formulae    :    4 (   2 unt;   0 def)
%            Number of atoms       :    8 (   0 equ)
%            Maximal formula atoms :    4 (   2 avg)
%            Number of connectives :    4 (   0   ~;   0   |;   2   &)
%                                         (   0 <=>;   2  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    7 (   5 avg)
%            Maximal term depth    :    2 (   1 avg)
%            Number of predicates  :    3 (   3 usr;   0 prp; 1-3 aty)
%            Number of functors    :    3 (   3 usr;   0 con; 2-3 aty)
%            Number of variables   :   13 (  13   !;   0   ?)
% SPC      : FOF_THM_RFO_NEQ

% Comments :
%------------------------------------------------------------------------------
%----The statement on the top of page 90 of Kelley
fof(kelley_p90a,axiom,
    ! [A,X,A1] : a_continuous_function_from_onto(the_projection_function(A,X,A1),the_product_top_space_over(X,A1),apply(X,A)) ).

%----Theorem 2 in chapter 3 of Kelley
fof(kelley_3_2,axiom,
    ! [A,X,A1,X1] : an_open_function_from_onto(the_projection_function(A,X,A1),the_product_top_space_over(X1,A1),apply(X1,A)) ).

%----A statement near the bottom of page 147 of Kelley
fof(kelley_p_147e,axiom,
    ! [F,A,B] :
      ( ( an_open_function_from_onto(F,A,B)
        & a_continuous_function_from_onto(F,A,B)
        & a_locally_compact_top_space(A) )
     => a_locally_compact_top_space(B) ) ).

%----The first half of Theorem 19 in Chapter 5 of Kelley
fof(kelley_5_19a,conjecture,
    ! [X1,A1] :
      ( a_locally_compact_top_space(the_product_top_space_over(X1,A1))
     => ! [A] : a_locally_compact_top_space(apply(X1,A)) ) ).

%------------------------------------------------------------------------------
