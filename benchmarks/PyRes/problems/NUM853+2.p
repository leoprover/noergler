
%------------------------------------------------------------------------------
% File     : NUM853+2 : TPTP v9.2.1. Released v4.1.0.
% Domain   : Number Theory
% Problem  : holds(conseq(307),490,0)
% Version  : Especial: Reduced > Especial.
% English  :

% Refs     : [Lan30] Landau (1930), Grundlagen der Analysis
%          : [Kue09] Kuehlwein (2009), Email to Geoff Sutcliffe
%          : [KC+10] Kuehlwein et al. (2010), Premise Selection in the Napr
% Source   : [Kue09]
% Names    :

% Status   : Theorem
% Rating   : 0.24 v9.1.0, 0.30 v9.0.0, 0.25 v8.2.0, 0.31 v7.4.0, 0.37 v7.3.0, 0.31 v7.2.0, 0.28 v7.1.0, 0.35 v7.0.0, 0.30 v6.4.0, 0.38 v6.3.0, 0.29 v6.2.0, 0.36 v6.1.0, 0.33 v6.0.0, 0.35 v5.5.0, 0.33 v5.4.0, 0.32 v5.3.0, 0.41 v5.2.0, 0.25 v5.1.0, 0.29 v4.1.0
% Syntax   : Number of formulae    :   37 (  14 unt;   0 def)
%            Number of atoms       :   63 (  37 equ)
%            Maximal formula atoms :    3 (   1 avg)
%            Number of connectives :   45 (  19   ~;  10   |;   3   &)
%                                         (   2 <=>;  11  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    7 (   4 avg)
%            Maximal term depth    :    3 (   1 avg)
%            Number of predicates  :    5 (   4 usr;   0 prp; 2-2 aty)
%            Number of functors    :    8 (   8 usr;   4 con; 0-2 aty)
%            Number of variables   :   82 (  74   !;   8   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : From the Landau in Naproche 0.45 collection.
%          : This version uses a filtered set of axioms.
%------------------------------------------------------------------------------
fof('holds(conseq(307), 490, 0)',conjecture,
    greater(vd486,vd488) ).

fof('holds(antec(307), 489, 0)',axiom,
    greater(vmul(vd486,vd487),vmul(vd488,vd487)) ).

fof('ass(cond(299, 0), 0)',axiom,
    ! [Vd456,Vd465,Vd466] :
      ( less(Vd465,Vd466)
     => less(vmul(Vd465,Vd456),vmul(Vd466,Vd456)) ) ).

fof('ass(cond(299, 0), 1)',axiom,
    ! [Vd456,Vd461,Vd462] :
      ( Vd461 = Vd462
     => vmul(Vd461,Vd456) = vmul(Vd462,Vd456) ) ).

fof('ass(cond(299, 0), 2)',axiom,
    ! [Vd456,Vd457,Vd458] :
      ( greater(Vd457,Vd458)
     => greater(vmul(Vd457,Vd456),vmul(Vd458,Vd456)) ) ).

fof('ass(cond(290, 0), 0)',axiom,
    ! [Vd444,Vd445,Vd446] : vmul(vmul(Vd444,Vd445),Vd446) = vmul(Vd444,vmul(Vd445,Vd446)) ).

fof('ass(cond(281, 0), 0)',axiom,
    ! [Vd432,Vd433,Vd434] : vmul(Vd432,vplus(Vd433,Vd434)) = vplus(vmul(Vd432,Vd433),vmul(Vd432,Vd434)) ).

fof('ass(cond(270, 0), 0)',axiom,
    ! [Vd418,Vd419] : vmul(Vd418,Vd419) = vmul(Vd419,Vd418) ).

fof('ass(cond(261, 0), 0)',axiom,
    ! [Vd408,Vd409] : vmul(vsucc(Vd408),Vd409) = vplus(vmul(Vd408,Vd409),Vd409) ).

fof('ass(cond(253, 0), 0)',axiom,
    ! [Vd400] : vmul(v1,Vd400) = Vd400 ).

fof('qu(cond(conseq(axiom(3)), 32), and(holds(definiens(249), 399, 0), holds(definiens(249), 398, 0)))',axiom,
    ! [Vd396,Vd397] :
      ( vmul(Vd396,vsucc(Vd397)) = vplus(vmul(Vd396,Vd397),Vd396)
      & vmul(Vd396,v1) = Vd396 ) ).

fof('ass(cond(241, 0), 0)',axiom,
    ! [Vd386,Vd387] :
      ( less(Vd386,vplus(Vd387,v1))
     => leq(Vd386,Vd387) ) ).

fof('ass(cond(234, 0), 0)',axiom,
    ! [Vd375,Vd376] :
      ( greater(Vd375,Vd376)
     => geq(Vd375,vplus(Vd376,v1)) ) ).

fof('ass(cond(228, 0), 0)',axiom,
    ! [Vd369] : geq(Vd369,v1) ).

fof('ass(cond(223, 0), 0)',axiom,
    ! [Vd362,Vd363,Vd365,Vd366] :
      ( ( geq(Vd365,Vd366)
        & geq(Vd362,Vd363) )
     => geq(vplus(Vd362,Vd365),vplus(Vd363,Vd366)) ) ).

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

%------------------------------------------------------------------------------
