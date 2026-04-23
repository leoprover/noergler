
%------------------------------------------------------------------------------
% File     : NUM854+2 : TPTP v9.2.1. Released v4.1.0.
% Domain   : Number Theory
% Problem  : holds(conjunct1(315),514,0)
% Version  : Especial: Reduced > Especial.
% English  :

% Refs     : [Lan30] Landau (1930), Grundlagen der Analysis
%          : [Kue09] Kuehlwein (2009), Email to Geoff Sutcliffe
%          : [KC+10] Kuehlwein et al. (2010), Premise Selection in the Napr
% Source   : [Kue09]
% Names    :

% Status   : Theorem
% Rating   : 0.09 v9.0.0, 0.11 v8.1.0, 0.17 v7.5.0, 0.19 v7.4.0, 0.17 v7.2.0, 0.14 v7.1.0, 0.17 v7.0.0, 0.13 v6.4.0, 0.19 v6.3.0, 0.17 v6.2.0, 0.20 v6.1.0, 0.23 v6.0.0, 0.17 v5.5.0, 0.19 v5.4.0, 0.18 v5.3.0, 0.26 v5.2.0, 0.05 v5.0.0, 0.08 v4.1.0
% Syntax   : Number of formulae    :   27 (   8 unt;   0 def)
%            Number of atoms       :   48 (  25 equ)
%            Maximal formula atoms :    3 (   1 avg)
%            Number of connectives :   33 (  12   ~;  10   |;   1   &)
%                                         (   2 <=>;   8  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    6 (   4 avg)
%            Maximal term depth    :    3 (   1 avg)
%            Number of predicates  :    3 (   2 usr;   0 prp; 2-2 aty)
%            Number of functors    :    8 (   8 usr;   5 con; 0-2 aty)
%            Number of variables   :   63 (  55   !;   8   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : From the Landau in Naproche 0.45 collection.
%          : This version uses a filtered set of axioms.
%------------------------------------------------------------------------------
fof('holds(conjunct1(315), 514, 0)',conjecture,
    greater(vmul(vd508,vd511),vmul(vd509,vd511)) ).

fof('holds(conjunct2(314), 513, 0)',axiom,
    greater(vd511,vd512) ).

fof('holds(conjunct1(314), 510, 0)',axiom,
    greater(vd508,vd509) ).

fof('ass(cond(conjunct2(conjunct2(307)), 0), 0)',axiom,
    ! [Vd496,Vd497,Vd498] :
      ( less(vmul(Vd496,Vd497),vmul(Vd498,Vd497))
     => less(Vd496,Vd498) ) ).

fof('ass(cond(conjunct1(conjunct2(307)), 0), 0)',axiom,
    ! [Vd491,Vd492,Vd493] :
      ( vmul(Vd491,Vd492) = vmul(Vd493,Vd492)
     => Vd491 = Vd493 ) ).

fof('ass(cond(conjunct1(307), 0), 0)',axiom,
    ! [Vd486,Vd487,Vd488] :
      ( greater(vmul(Vd486,Vd487),vmul(Vd488,Vd487))
     => greater(Vd486,Vd488) ) ).

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

%------------------------------------------------------------------------------
