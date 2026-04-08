---
stepsCompleted: [1, 2, 3, 4, 5, 6]
inputDocuments: []
date: 2026-03-28
author: Gino Cachondeo
---

# Product Brief: FocusLock iPhone

## Vision produit

FocusLock aide une personne a reprendre le controle de son attention sur iPhone en limitant l'acces a des applications et a des sites distrayants selon des budgets de temps, des plages horaires et des regles de blocage volontaire.

## Probleme

Les outils natifs de gestion du temps d'ecran sont juges trop generiques, peu motivants, et insuffisamment centres sur l'engagement personnel. L'utilisateur veut une solution plus orientee discipline personnelle:

- limiter Instagram, TikTok ou Safari a un quota quotidien
- bloquer des sites choisis
- rendre le contournement plus difficile
- suivre ses progres sans experience punitive

## Utilisateurs cibles

### Persona principal

- Adulte en auto-discipline numerique
- utilise un iPhone comme appareil principal
- veut reduire les usages compulsifs sans demander un controle parental classique

### Persona secondaire

- Etudiant ou freelance en recherche de concentration
- veut creer des sessions de focus et couper les distractions

## Proposition de valeur

Une application de digital wellbeing orientee engagement personnel qui combine:

- quotas journaliers simples a configurer
- blocage d'apps, categories et domaines web
- rituels de friction avant contournement
- tableau de bord de progression lisible
- experience moderne et rassurante

## Hypothese structurante

Une PWA seule ne peut pas imposer sur iPhone le blocage systeme d'autres apps ou de domaines web. Le produit sera donc concu comme:

- une interface Angular/PWA pour onboarding, compte, regles et visualisation
- un backend Spring pour persistance, synchronisation et analytics
- une couche iOS native indispensable pour appliquer les restrictions via les API Screen Time d'Apple

## Objectifs business et produit

- aider l'utilisateur a respecter ses limites d'usage quotidiennes
- transformer la configuration de blocage en routine simple et rassurante
- creer une experience plus motivante que les reglages iOS standard
- offrir un socle extensible vers abonnements premium, comptes multi-appareils et coaching

## Metriques de succes

- taux d'activation: utilisateur ayant autorise les droits iOS et configure au moins une regle
- time-to-value: temps jusqu'a la premiere regle active
- taux de respect des limites quotidiennes
- nombre moyen de contournements par semaine
- retention J7 et J30
- nombre de regles actives par utilisateur

## Portee MVP

- onboarding iPhone et explication des permissions
- selection d'apps/categories/sites a restreindre
- quotas journaliers par app, categorie ou groupe
- blocage planifie par plage horaire
- ecran de blocage personnalise
- dashboard simple de suivi
- override volontaire avec friction

## Hors MVP

- compatibilite Android
- blocage poste de travail desktop
- controle parental multi-enfants
- AI coaching complexe
- marketplace de programmes de focus

## Risques et contraintes

- dependance forte aux capacites et entitlement Apple Screen Time
- impossibilite d'obtenir un blocage systeme fiable avec une PWA pure
- risque de rejet App Store si le positionnement, permissions ou promesses sont mal cadres
- risque UX eleve si l'on promet plus que ce qu'iOS permet reellement

## Positionnement UX

Le produit doit evoquer la maitrise, le calme et la determination, pas la culpabilite. Le ton doit etre adulte, direct et motivant.

