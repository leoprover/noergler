
%------------------------------------------------------------------------------
% File     : PUZ130+1 : TPTP v9.2.1. Released v4.1.0.
% Domain   : Puzzles
% Problem  : Garfield and Odie
% Version  : Especial.
% English  : Garfield is a cat and Odie is a dog. Cats and dogs are pets. Jon
%            is a human. Every pet has a human owner. Jon owns Garfield and
%            Odie, and they are the only cat and dog he owns. If a dog chases
%            a cat, then the cat's owner hates the dog's owner. Odie has chased
%            Garfield. Therefore, Jon hates himself.

% Refs     : 
% Source   : [TPTP]
% Names    :

% Status   : Theorem
% Rating   : 0.09 v9.1.0, 0.06 v9.0.0, 0.11 v8.2.0, 0.08 v8.1.0, 0.11 v7.5.0, 0.12 v7.4.0, 0.03 v7.1.0, 0.04 v7.0.0, 0.03 v6.4.0, 0.08 v6.3.0, 0.04 v6.2.0, 0.08 v6.1.0, 0.13 v6.0.0, 0.04 v5.3.0, 0.11 v5.2.0, 0.00 v4.1.0
% Syntax   : Number of formulae    :   19 (  11 unt;   0 def)
%            Number of atoms       :   34 (   3 equ)
%            Maximal formula atoms :    4 (   1 avg)
%            Number of connectives :   15 (   0   ~;   0   |;   3   &)
%                                         (   1 <=>;  11  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    5 (   3 avg)
%            Maximal term depth    :    2 (   1 avg)
%            Number of predicates  :    8 (   7 usr;   0 prp; 1-2 aty)
%            Number of functors    :    4 (   4 usr;   3 con; 0-1 aty)
%            Number of variables   :   17 (  12   !;   5   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments :
%------------------------------------------------------------------------------
fof(cat_type,axiom,
    ? [A] : cat(A) ).

fof(garfield_type,axiom,
    cat(garfield) ).

fof(dog_type,axiom,
    ? [A] : dog(A) ).

fof(odie_type,axiom,
    dog(odie) ).

fof(pet_type,axiom,
    ? [A] : pet(A) ).

fof(dog_pet_type,axiom,
    ! [A] :
      ( dog(A)
     => pet(A) ) ).

fof(cat_pet_type,axiom,
    ! [A] :
      ( cat(A)
     => pet(A) ) ).

fof(human_type,axiom,
    ? [A] : human(A) ).

fof(jon_type,axiom,
    human(jon) ).

fof(owner_of_type,axiom,
    ! [A] :
      ( pet(A)
     => human(owner_of(A)) ) ).

fof(pet_owner_axiom,axiom,
    ! [X] :
      ( pet(X)
     => ? [Y] :
          ( human(Y)
          & owner(X,Y) ) ) ).

fof(jon_o_owner_axiom,axiom,
    owner(jon,odie) ).

fof(jon_g_owner_axiom,axiom,
    owner(jon,garfield) ).

fof(jon_only_g_owner_axiom,axiom,
    ! [X] :
      ( ! [X] :
          ( cat(X)
         => owner(jon,X) )
     => X = garfield ) ).

fof(jon_only_o_owner_axiom,axiom,
    ! [X] :
      ( ! [X] :
          ( dog(X)
         => owner(jon,X) )
     => X = odie ) ).

fof(cat_chase_axiom,axiom,
    ! [X,Y] :
      ( ( cat(X)
        & dog(Y) )
     => ( chased(Y,X)
       => hates(owner_of(X),owner_of(Y)) ) ) ).

fof(owner_def,axiom,
    ! [X,Y] :
      ( ( human(X)
        & pet(Y) )
     => ( owner(X,Y)
      <=> X = owner_of(Y) ) ) ).

fof(odie_chase_axiom,axiom,
    chased(odie,garfield) ).

fof(jon_conjecture,conjecture,
    hates(jon,jon) ).

%------------------------------------------------------------------------------
