
%------------------------------------------------------------------------------
% File     : NUM838+1 : TPTP v9.2.1. Released v4.1.0.
% Domain   : Number Theory
% Problem  : holds(conseq(195),305,0)
% Version  : Especial.
% English  :

% Refs     : [Lan30] Landau (1930), Grundlagen der Analysis
%          : [Kue09] Kuehlwein (2009), Email to Geoff Sutcliffe
%          : [KC+10] Kuehlwein et al. (2010), Premise Selection in the Napr
% Source   : [Kue09]
% Names    :

% Status   : Theorem
% Rating   : 0.09 v9.0.0, 0.11 v8.1.0, 0.17 v7.5.0, 0.19 v7.4.0, 0.13 v7.3.0, 0.17 v7.2.0, 0.14 v7.1.0, 0.17 v7.0.0, 0.13 v6.4.0, 0.19 v6.3.0, 0.17 v6.2.0, 0.20 v6.0.0, 0.22 v5.5.0, 0.15 v5.4.0, 0.14 v5.3.0, 0.22 v5.2.0, 0.05 v5.0.0, 0.08 v4.1.0
% Syntax   : Number of formulae    :   44 (  10 unt;   0 def)
%            Number of atoms       :   87 (  40 equ)
%            Maximal formula atoms :    5 (   1 avg)
%            Number of connectives :   63 (  20   ~;  13   |;   5   &)
%                                         (   4 <=>;  21  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    7 (   5 avg)
%            Maximal term depth    :    4 (   1 avg)
%            Number of predicates  :    5 (   4 usr;   0 prp; 2-2 aty)
%            Number of functors    :    8 (   8 usr;   4 con; 0-2 aty)
%            Number of variables   :  103 (  95   !;   8   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : From the Landau in Naproche 0.45 collection.
%------------------------------------------------------------------------------
fof('holds(conseq(195), 305, 0)',conjecture,
    greater(vplus(vd301,vd303),vplus(vd302,vd303)) ).

fof('holds(antec(195), 304, 0)',axiom,
    greater(vd301,vd302) ).

fof('ass(cond(proof(196), 0), 0)',axiom,
    ! [Vd310,Vd322,Vd323] :
      ( less(Vd322,Vd323)
     => less(vplus(Vd322,Vd310),vplus(Vd323,Vd310)) ) ).

fof('ass(cond(proof(196), 0), 1)',axiom,
    ! [Vd310,Vd322,Vd323] :
      ( less(Vd322,Vd323)
     => greater(vplus(Vd323,Vd310),vplus(Vd322,Vd310)) ) ).

fof('ass(cond(proof(196), 0), 2)',axiom,
    ! [Vd310,Vd322,Vd323] :
      ( less(Vd322,Vd323)
     => greater(Vd323,Vd322) ) ).

fof('ass(cond(proof(196), 0), 3)',axiom,
    ! [Vd310,Vd318,Vd319] :
      ( Vd318 = Vd319
     => vplus(Vd318,Vd310) = vplus(Vd319,Vd310) ) ).

fof('ass(cond(proof(196), 0), 4)',axiom,
    ! [Vd310,Vd311,Vd312] :
      ( greater(Vd311,Vd312)
     => greater(vplus(Vd311,Vd310),vplus(Vd312,Vd310)) ) ).

fof('ass(cond(proof(196), 0), 5)',axiom,
    ! [Vd310,Vd311,Vd312] :
      ( greater(Vd311,Vd312)
     => vplus(vskolem7(Vd311,Vd312),vplus(Vd312,Vd310)) = vplus(vplus(Vd312,Vd310),vskolem7(Vd311,Vd312)) ) ).

fof('ass(cond(proof(196), 0), 6)',axiom,
    ! [Vd310,Vd311,Vd312] :
      ( greater(Vd311,Vd312)
     => vplus(vplus(vskolem7(Vd311,Vd312),Vd312),Vd310) = vplus(vskolem7(Vd311,Vd312),vplus(Vd312,Vd310)) ) ).

fof('ass(cond(proof(196), 0), 7)',axiom,
    ! [Vd310,Vd311,Vd312] :
      ( greater(Vd311,Vd312)
     => vplus(vplus(Vd312,vskolem7(Vd311,Vd312)),Vd310) = vplus(vplus(vskolem7(Vd311,Vd312),Vd312),Vd310) ) ).

fof('ass(cond(proof(196), 0), 8)',axiom,
    ! [Vd310,Vd311,Vd312] :
      ( greater(Vd311,Vd312)
     => vplus(Vd311,Vd310) = vplus(vplus(Vd312,vskolem7(Vd311,Vd312)),Vd310) ) ).

fof('ass(cond(proof(196), 0), 9)',axiom,
    ! [Vd310,Vd311,Vd312] :
      ( greater(Vd311,Vd312)
     => Vd311 = vplus(Vd312,vskolem7(Vd311,Vd312)) ) ).

fof('ass(cond(189, 0), 0)',axiom,
    ! [Vd295,Vd296] : greater(vplus(Vd295,Vd296),Vd295) ).

fof('ass(cond(184, 0), 0)',axiom,
    ! [Vd289,Vd290,Vd292] :
      ( ( leq(Vd290,Vd292)
        & leq(Vd289,Vd290) )
     => leq(Vd289,Vd292) ) ).

fof('ass(cond(goal(177), 0), 0)',axiom,
    ! [Vd281,Vd282,Vd283] :
      ( ( ( less(Vd282,Vd283)
          & leq(Vd281,Vd282) )
        | ( leq(Vd282,Vd283)
          & less(Vd281,Vd282) ) )
     => less(Vd281,Vd283) ) ).

fof('ass(cond(168, 0), 0)',axiom,
    ! [Vd262,Vd263,Vd265] :
      ( ( less(Vd263,Vd265)
        & less(Vd262,Vd263) )
     => less(Vd262,Vd265) ) ).

fof('ass(cond(163, 0), 0)',axiom,
    ! [Vd258,Vd259] :
      ( leq(Vd258,Vd259)
     => geq(Vd259,Vd258) ) ).

fof('ass(cond(158, 0), 0)',axiom,
    ! [Vd254,Vd255] :
      ( geq(Vd254,Vd255)
     => leq(Vd255,Vd254) ) ).

fof('def(cond(conseq(axiom(3)), 17), 1)',axiom,
    ! [Vd249,Vd250] :
      ( leq(Vd250,Vd249)
    <=> ( less(Vd250,Vd249)
        | Vd250 = Vd249 ) ) ).

fof('def(cond(conseq(axiom(3)), 16), 1)',axiom,
    ! [Vd244,Vd245] :
      ( geq(Vd245,Vd244)
    <=> ( greater(Vd245,Vd244)
        | Vd245 = Vd244 ) ) ).

fof('ass(cond(147, 0), 0)',axiom,
    ! [Vd226,Vd227] :
      ( less(Vd226,Vd227)
     => greater(Vd227,Vd226) ) ).

fof('ass(cond(140, 0), 0)',axiom,
    ! [Vd208,Vd209] :
      ( greater(Vd208,Vd209)
     => less(Vd209,Vd208) ) ).

fof('ass(cond(goal(130), 0), 0)',axiom,
    ! [Vd203,Vd204] :
      ( Vd203 = Vd204
      | greater(Vd203,Vd204)
      | less(Vd203,Vd204) ) ).

fof('ass(cond(goal(130), 0), 1)',axiom,
    ! [Vd203,Vd204] :
      ( Vd203 != Vd204
      | ~ less(Vd203,Vd204) ) ).

fof('ass(cond(goal(130), 0), 2)',axiom,
    ! [Vd203,Vd204] :
      ( ~ greater(Vd203,Vd204)
      | ~ less(Vd203,Vd204) ) ).

fof('ass(cond(goal(130), 0), 3)',axiom,
    ! [Vd203,Vd204] :
      ( Vd203 != Vd204
      | ~ greater(Vd203,Vd204) ) ).

fof('def(cond(conseq(axiom(3)), 12), 1)',axiom,
    ! [Vd198,Vd199] :
      ( less(Vd199,Vd198)
    <=> ? [Vd201] : Vd198 = vplus(Vd199,Vd201) ) ).

fof('def(cond(conseq(axiom(3)), 11), 1)',axiom,
    ! [Vd193,Vd194] :
      ( greater(Vd194,Vd193)
    <=> ? [Vd196] : Vd194 = vplus(Vd193,Vd196) ) ).

fof('ass(cond(goal(88), 0), 0)',axiom,
    ! [Vd120,Vd121] :
      ( Vd120 = Vd121
      | ? [Vd123] : Vd120 = vplus(Vd121,Vd123)
      | ? [Vd125] : Vd121 = vplus(Vd120,Vd125) ) ).

fof('ass(cond(goal(88), 0), 1)',axiom,
    ! [Vd120,Vd121] :
      ( Vd120 != Vd121
      | ~ ? [Vd125] : Vd121 = vplus(Vd120,Vd125) ) ).

fof('ass(cond(goal(88), 0), 2)',axiom,
    ! [Vd120,Vd121] :
      ( ~ ? [Vd123] : Vd120 = vplus(Vd121,Vd123)
      | ~ ? [Vd125] : Vd121 = vplus(Vd120,Vd125) ) ).

fof('ass(cond(goal(88), 0), 3)',axiom,
    ! [Vd120,Vd121] :
      ( Vd120 != Vd121
      | ~ ? [Vd123] : Vd120 = vplus(Vd121,Vd123) ) ).

fof('ass(cond(81, 0), 0)',axiom,
    ! [Vd104,Vd105] :
      ( Vd104 != Vd105
     => ! [Vd107] : vplus(Vd107,Vd104) != vplus(Vd107,Vd105) ) ).

fof('ass(cond(73, 0), 0)',axiom,
    ! [Vd92,Vd93] : Vd93 != vplus(Vd92,Vd93) ).

fof('ass(cond(61, 0), 0)',axiom,
    ! [Vd78,Vd79] : vplus(Vd79,Vd78) = vplus(Vd78,Vd79) ).

fof('ass(cond(52, 0), 0)',axiom,
    ! [Vd68,Vd69] : vplus(vsucc(Vd68),Vd69) = vsucc(vplus(Vd68,Vd69)) ).

fof('ass(cond(43, 0), 0)',axiom,
    ! [Vd59] : vplus(v1,Vd59) = vsucc(Vd59) ).

fof('ass(cond(33, 0), 0)',axiom,
    ! [Vd46,Vd47,Vd48] : vplus(vplus(Vd46,Vd47),Vd48) = vplus(Vd46,vplus(Vd47,Vd48)) ).

fof('qu(cond(conseq(axiom(3)), 3), and(holds(definiens(29), 45, 0), holds(definiens(29), 44, 0)))',axiom,
    ! [Vd42,Vd43] :
      ( vplus(Vd42,vsucc(Vd43)) = vsucc(vplus(Vd42,Vd43))
      & vplus(Vd42,v1) = vsucc(Vd42) ) ).

fof('ass(cond(20, 0), 0)',axiom,
    ! [Vd24] :
      ( Vd24 != v1
     => Vd24 = vsucc(vskolem2(Vd24)) ) ).

fof('ass(cond(12, 0), 0)',axiom,
    ! [Vd16] : vsucc(Vd16) != Vd16 ).

fof('ass(cond(6, 0), 0)',axiom,
    ! [Vd7,Vd8] :
      ( Vd7 != Vd8
     => vsucc(Vd7) != vsucc(Vd8) ) ).

fof('qu(antec(axiom(3)), imp(antec(axiom(3))))',axiom,
    ! [Vd3,Vd4] :
      ( vsucc(Vd3) = vsucc(Vd4)
     => Vd3 = Vd4 ) ).

fof('qu(restrictor(axiom(1)), holds(scope(axiom(1)), 2, 0))',axiom,
    ! [Vd1] : vsucc(Vd1) != v1 ).

%------------------------------------------------------------------------------
