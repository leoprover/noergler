
%------------------------------------------------------------------------------
% File     : PUZ132+1 : TPTP v9.2.1. Released v4.1.0.
% Domain   : Puzzles
% Problem  : Crime in beautiful Washington
% Version  : Especial.
% English  : A capital is a city. USA is a country. Every city has crime.
%            Washington is the capital of the USA. Every country has a
%            beautiful capital. Therefore, Washington is beautiful but has
%            crime.

% Refs     :
% Source   : [TPTP]
% Names    :

% Status   : Theorem
% Rating   : 0.03 v9.1.0, 0.00 v9.0.0, 0.03 v7.1.0, 0.04 v7.0.0, 0.03 v6.4.0, 0.08 v6.3.0, 0.00 v6.2.0, 0.04 v6.1.0, 0.07 v6.0.0, 0.00 v5.3.0, 0.04 v5.2.0, 0.00 v4.1.0
% Syntax   : Number of formulae    :   11 (   6 unt;   0 def)
%            Number of atoms       :   16 (   1 equ)
%            Maximal formula atoms :    2 (   1 avg)
%            Number of connectives :    5 (   0   ~;   0   |;   1   &)
%                                         (   0 <=>;   4  =>;   0  <=;   0 <~>)
%            Maximal formula depth :    3 (   2 avg)
%            Maximal term depth    :    2 (   1 avg)
%            Number of predicates  :    6 (   5 usr;   0 prp; 1-2 aty)
%            Number of functors    :    3 (   3 usr;   2 con; 0-1 aty)
%            Number of variables   :    7 (   4   !;   3   ?)
% SPC      : FOF_THM_RFO_SEQ

% Comments :
%------------------------------------------------------------------------------
fof(capital_type,axiom,
    ? [A] : capital(A) ).

fof(city_type,axiom,
    ? [A] : city(A) ).

fof(capital_city_type,axiom,
    ! [A] :
      ( capital(A)
     => city(A) ) ).

fof(country_type,axiom,
    ? [A] : country(A) ).

fof(washington_type,axiom,
    capital(washington) ).

fof(usa_type,axiom,
    country(usa) ).

fof(country_capital_type,axiom,
    ! [A] :
      ( country(A)
     => capital(capital_city(A)) ) ).

fof(crime_axiom,axiom,
    ! [X] :
      ( city(X)
     => has_crime(X) ) ).

fof(usa_capital_axiom,axiom,
    capital_city(usa) = washington ).

fof(beautiful_capital_axiom,axiom,
    ! [X] :
      ( country(X)
     => beautiful(capital_city(X)) ) ).

fof(washington_conjecture,conjecture,
    ( beautiful(washington)
    & has_crime(washington) ) ).

%------------------------------------------------------------------------------
