
%------------------------------------------------------------------------------
% File     : NUM844+2 : TPTP v9.2.1. Released v4.1.0.
% Domain   : Number Theory
% Problem  : holds(266,415,3)
% Version  : Especial: Reduced > Especial.
% English  :

% Refs     : [Lan30] Landau (1930), Grundlagen der Analysis
%          : [Kue09] Kuehlwein (2009), Email to Geoff Sutcliffe
%          : [KC+10] Kuehlwein et al. (2010), Premise Selection in the Napr
% Source   : [Kue09]
% Names    :

% Status   : Theorem
% Rating   : 0.06 v9.0.0, 0.08 v8.1.0, 0.14 v7.5.0, 0.16 v7.4.0, 0.13 v7.3.0, 0.14 v7.1.0, 0.22 v7.0.0, 0.17 v6.4.0, 0.23 v6.3.0, 0.25 v6.2.0, 0.28 v6.1.0, 0.33 v6.0.0, 0.30 v5.5.0, 0.22 v5.4.0, 0.25 v5.3.0, 0.30 v5.2.0, 0.10 v5.0.0, 0.21 v4.1.0
% Syntax   : Number of formulae    :   20 (  14 unt;   0 def)
%            Number of atoms       :   26 (  22 equ)
%            Maximal formula atoms :    2 (   1 avg)
%            Number of connectives :   10 (   4   ~;   0   |;   2   &)
%                                         (   0 <=>;   4  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    5 (   3 avg)
%            Maximal term depth    :    4 (   2 avg)
%            Number of predicates  :    5 (   4 usr;   0 prp; 2-2 aty)
%            Number of functors    :    7 (   7 usr;   3 con; 0-2 aty)
%            Number of variables   :   21 (  21   !;   0   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments : From the Landau in Naproche 0.45 collection.
%          : This version uses a filtered set of axioms.
%------------------------------------------------------------------------------
fof('holds(266, 415, 3)',conjecture,
    vplus(vmul(vd411,vd413),vplus(vd413,vsucc(vd411))) = vplus(vmul(vd411,vd413),vplus(vsucc(vd411),vd413)) ).

fof('holds(266, 415, 2)',axiom,
    vplus(vplus(vmul(vd411,vd413),vd413),vsucc(vd411)) = vplus(vmul(vd411,vd413),vplus(vd413,vsucc(vd411))) ).

fof('holds(266, 415, 1)',axiom,
    vplus(vmul(vsucc(vd411),vd413),vsucc(vd411)) = vplus(vplus(vmul(vd411,vd413),vd413),vsucc(vd411)) ).

fof('holds(266, 415, 0)',axiom,
    vmul(vsucc(vd411),vsucc(vd413)) = vplus(vmul(vsucc(vd411),vd413),vsucc(vd411)) ).

fof('holds(265, 414, 0)',axiom,
    vmul(vsucc(vd411),vd413) = vplus(vmul(vd411,vd413),vd413) ).

fof('holds(264, 412, 2)',axiom,
    vsucc(vmul(vd411,v1)) = vplus(vmul(vd411,v1),v1) ).

fof('holds(264, 412, 1)',axiom,
    vsucc(vd411) = vsucc(vmul(vd411,v1)) ).

fof('holds(264, 412, 0)',axiom,
    vmul(vsucc(vd411),v1) = vsucc(vd411) ).

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
