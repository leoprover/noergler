
%--------------------------------------------------------------------------
% File     : SET585+3 : TPTP v9.2.1. Released v2.2.0.
% Domain   : Set Theory
% Problem  : The intersection of X and Y is a subset of the union of X and Z
% Version  : [Try90] axioms : Reduced > Incomplete.
% English  :

% Refs     : [ILF] The ILF Group (1998), The ILF System: A Tool for the Int
%          : [Try90] Trybulec (1990), Tarski Grothendieck Set Theory
%          : [TS89]  Trybulec & Swieczkowska (1989), Boolean Properties of
% Source   : [ILF]
% Names    : BOOLE (38) [TS89]

% Status   : Theorem
% Rating   : 0.06 v9.1.0, 0.00 v9.0.0, 0.03 v8.1.0, 0.00 v6.4.0, 0.04 v6.3.0, 0.00 v6.1.0, 0.07 v6.0.0, 0.13 v5.5.0, 0.07 v5.4.0, 0.11 v5.3.0, 0.15 v5.2.0, 0.00 v5.0.0, 0.08 v4.1.0, 0.09 v4.0.1, 0.13 v4.0.0, 0.12 v3.7.0, 0.10 v3.5.0, 0.11 v3.4.0, 0.05 v3.3.0, 0.14 v3.2.0, 0.09 v3.1.0, 0.11 v2.7.0, 0.00 v2.2.1
% Syntax   : Number of formulae    :   11 (   6 unt;   0 def)
%            Number of atoms       :   21 (   3 equ)
%            Maximal formula atoms :    3 (   1 avg)
%            Number of connectives :   10 (   0   ~;   1   |;   2   &)
%                                         (   5 <=>;   2  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    6 (   4 avg)
%            Maximal term depth    :    2 (   1 avg)
%            Number of predicates  :    3 (   2 usr;   0 prp; 2-2 aty)
%            Number of functors    :    2 (   2 usr;   0 con; 2-2 aty)
%            Number of variables   :   27 (  27   !;   0   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments :
%--------------------------------------------------------------------------
%---- line(boole - th(29),1833172)
fof(transitivity_of_subset,axiom,
    ! [B,C,D] :
      ( ( subset(B,C)
        & subset(C,D) )
     => subset(B,D) ) ).

%---- line(boole - th(31),1833190)
fof(subset_of_union,axiom,
    ! [B,C] : subset(B,union(B,C)) ).

%---- line(boole - th(37),1833277)
fof(intersection_is_subset,axiom,
    ! [B,C] : subset(intersection(B,C),B) ).

%---- line(boole - df(2),1833042)
fof(union_defn,axiom,
    ! [B,C,D] :
      ( member(D,union(B,C))
    <=> ( member(D,B)
        | member(D,C) ) ) ).

%---- line(boole - df(3),1833060)
fof(intersection_defn,axiom,
    ! [B,C,D] :
      ( member(D,intersection(B,C))
    <=> ( member(D,B)
        & member(D,C) ) ) ).

%---- line(tarski - df(3),1832749)
fof(subset_defn,axiom,
    ! [B,C] :
      ( subset(B,C)
    <=> ! [D] :
          ( member(D,B)
         => member(D,C) ) ) ).

%---- property(commutativity,op(union,2,function))
fof(commutativity_of_union,axiom,
    ! [B,C] : union(B,C) = union(C,B) ).

%---- property(commutativity,op(intersection,2,function))
fof(commutativity_of_intersection,axiom,
    ! [B,C] : intersection(B,C) = intersection(C,B) ).

%---- property(reflexivity,op(subset,2,predicate))
fof(reflexivity_of_subset,axiom,
    ! [B] : subset(B,B) ).

%---- line(hidden - axiom47,1832615)
fof(equal_member_defn,axiom,
    ! [B,C] :
      ( B = C
    <=> ! [D] :
          ( member(D,B)
        <=> member(D,C) ) ) ).

%---- line(boole - th(38),1833287)
fof(prove_intersection_subset_of_union,conjecture,
    ! [B,C,D] : subset(intersection(B,C),union(B,D)) ).

%--------------------------------------------------------------------------
