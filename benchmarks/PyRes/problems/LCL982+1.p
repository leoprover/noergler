
%--------------------------------------------------------------------------
% File     : LCL982+1 : TPTP v9.2.1. Released v9.1.0.
% Domain   : Logic Calculi (Implication/Negation 2 valued sentential)
% Problem  : CN-1 depends on the single Walsh/Fitelson axiom A5
% Version  : [WF21] axioms.
% English  : 

% Refs     : [MW92]  McCune & Wos (1992), Experiments in Automated Deductio
%          : [WF21]  Walsh & Fitelson (2021),  Answers to Some Open Questio
% Source   : [WF21]
% Names    : A5 [WF21]

% Status   : Theorem
% Rating   : 0.35 v9.1.0
% Syntax   : Number of formulae    :    3 (   2 unt;   0 def)
%            Number of atoms       :    5 (   0 equ)
%            Maximal formula atoms :    3 (   1 avg)
%            Number of connectives :    4 (   2   ~;   2   |;   0   &)
%                                         (   0 <=>;   0  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    6 (   5 avg)
%            Maximal term depth    :    7 (   2 avg)
%            Number of predicates  :    1 (   1 usr;   0 prp; 1-1 aty)
%            Number of functors    :    2 (   2 usr;   0 con; 1-2 aty)
%            Number of variables   :   10 (  10   !;   0   ?)
% SPC      : FOF_THM_RFO_NEQ

% Comments :
%--------------------------------------------------------------------------
fof(condensed_detachment,axiom,
    ! [X,Y] :
      ( ~ is_a_theorem(implies(X,Y))
      | ~ is_a_theorem(X)
      | is_a_theorem(Y) ) ).

fof(a5,axiom,
    ! [A,B,C,D,E] :
      is_a_theorem(implies(implies(A,B),implies(implies(implies(C,implies(D,E)),implies(B,implies(not(D),not(A)))),implies(A,D)))) ).

fof(prove_cn_1,conjecture,
    ! [A,B,C] :
      is_a_theorem(implies(implies(A,B),implies(implies(B,C),implies(A,C)))) ).

%--------------------------------------------------------------------------
