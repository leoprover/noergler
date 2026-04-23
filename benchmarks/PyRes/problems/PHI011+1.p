
%------------------------------------------------------------------------------
% File     : PHI011+1 : TPTP v9.2.1. Released v7.2.0.
% Domain   : Philosophy
% Problem  : Lemma for Anselm's ontological argument
% Version  : [Wol16] axioms.
% English  :

% Refs     : [OZ11]  Oppenheimer & Zalta (2011), A Computationally-Discover
%          : [Wol16] Woltzenlogel Paleo (2016), Email to Geoff Sutcliffe
% Source   : [Wol16]
% Names    : descripthm2.p [Wol16]

% Status   : Theorem
% Rating   : 0.00 v7.2.0
% Syntax   : Number of formulae    :    5 (   0 unt;   0 def)
%            Number of atoms       :   20 (   1 equ)
%            Maximal formula atoms :    6 (   4 avg)
%            Number of connectives :   16 (   1   ~;   0   |;   6   &)
%                                         (   0 <=>;   9  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    7 (   6 avg)
%            Maximal term depth    :    1 (   1 avg)
%            Number of predicates  :    5 (   4 usr;   0 prp; 1-2 aty)
%            Number of functors    :    0 (   0 usr;   0 con; --- aty)
%            Number of variables   :   11 (  10   !;   1   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : See http://mally.stanford.edu/cm/ontological-argument/
%------------------------------------------------------------------------------
fof(objects_are_not_properties,axiom,
    ! [X] :
      ( object(X)
     => ~ property(X) ) ).

fof(exemplifier_is_object_and_exemplified_is_property,axiom,
    ! [X,F] :
      ( exemplifies_property(F,X)
     => ( property(F)
        & object(X) ) ) ).

fof(description_is_property_and_described_is_object,axiom,
    ! [X,F] :
      ( is_the(X,F)
     => ( property(F)
        & object(X) ) ) ).

fof(lemma_1,axiom,
    ! [X,F,Y] :
      ( ( object(X)
        & property(F)
        & object(Y) )
     => ( ( is_the(X,F)
          & X = Y )
       => exemplifies_property(F,Y) ) ) ).

fof(description_theorem_2,conjecture,
    ! [F] :
      ( property(F)
     => ( ? [Y] :
            ( object(Y)
            & is_the(Y,F) )
       => ! [Z] :
            ( object(Z)
           => ( is_the(Z,F)
             => exemplifies_property(F,Z) ) ) ) ) ).

%------------------------------------------------------------------------------
