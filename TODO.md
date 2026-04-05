# TODOs

## Allgemeine Gedanken/Ideen/Todos

  - [Melanie] check GDV-LP für weitere ideen für beweischecks
  - include LambdaPi?
  - wofür wird ask von Geoff benutzt?
  - Testen
    + auf vielen beweisen mal testen
    + insbesondere adversial attacks testen: beweisen manuell kaputt machen
    + kleine probleme/beweise aus TPTP/TSTP extrahieren? Müssen ggf. weitere Flags einbauen in Nörgler, damit
      gewisse Checks übersprungen werden können (z.B. kein Beweiser wird korrekte Skolemization inference-records
      ausspucken)

## Spezifische Checks

  - check negated conjecture
    + ist PNF check für negate_conjecture sinnvoll? oder lieber als externer check?
  - Skolemization
    + [Alex, done] check annotation information für skolemization
  - Generic inference check
    + [Happy] speziell: welcher model finder? nitpick, paradox, mace4, FEST, cvc5 ...? alloy
    + portfolio aus beweisern? oder nach-und-nach, gaveup->next?, ...? allgemeiner ablauf optimieren

## Softwarearchitekturverbesserungen

  - parallelism, wie funktioniert das mit verschiedenen cores?
