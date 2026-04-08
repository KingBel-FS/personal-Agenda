---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7]
inputDocuments:
  - C:\Dev\blocker\_bmad-output\planning-artifacts\product-brief-focuslock-ios-2026-03-28.md
date: 2026-03-28
author: Gino Cachondeo
---

# PRD: FocusLock iPhone

## Resume executif

FocusLock est un produit de self-control numerique pour iPhone. L'utilisateur installe une interface Angular/PWA pour piloter son experience, mais l'application de restriction repose sur une app iOS native connectee a un backend Spring. Le MVP doit permettre de definir des limites de temps et des blocages pour des apps et des sites distrayants, avec un parcours d'activation clair et une transparence totale sur les limites techniques.

## Objectifs

- permettre de limiter une app comme Instagram a 30 minutes par jour
- permettre de bloquer des domaines web ou categories web depuis l'iPhone
- reduire la charge mentale de configuration
- proposer une experience plus motivante que les reglages natifs

## Non-objectifs

- promettre un blocage universel depuis une simple PWA
- couvrir Android ou desktop au MVP
- remplacer completement les reglages systeme Apple

## Exigences fonctionnelles

### FR1. Onboarding et eligibilite

Le systeme doit expliquer des le debut que l'application cible l'iPhone et que l'application effective des restrictions necessite les autorisations Apple adequates.

### FR2. Connexion et profil

Le systeme doit permettre a un utilisateur de creer un compte, se connecter et retrouver ses regles et preferes.

### FR3. Activation des permissions iOS

Le systeme doit guider l'utilisateur pour activer les capacites Screen Time necessaires et verifier l'etat de l'autorisation.

### FR4. Selection des cibles de restriction

Le systeme doit permettre de selectionner:

- des applications
- des categories d'applications
- des domaines web
- des categories web si supportees par la couche native

### FR5. Budgets quotidiens

Le systeme doit permettre de definir une limite quotidienne par cible ou groupe de cibles, par exemple Instagram 30 minutes par jour.

### FR6. Plages horaires de blocage

Le systeme doit permettre de bloquer certaines cibles sur des horaires recurrents, par exemple de 09:00 a 18:00 en semaine.

### FR7. Ecran de shield personnalise

Le systeme doit afficher un ecran de blocage coherent avec la marque et indiquer:

- pourquoi l'acces est bloque
- combien de temps reste avant la levee du blocage
- quelle action de contournement est autorisee, si disponible

### FR8. Contournement volontaire avec friction

Le systeme doit permettre un override sous conditions avec friction configurable, par exemple delai, justification ou double confirmation.

### FR9. Tableau de bord

Le systeme doit montrer:

- le temps consomme aujourd'hui
- les limites depassees ou proches
- les sessions respectees
- l'historique recent

### FR10. Notifications

Le systeme doit notifier l'utilisateur quand:

- une limite est presque atteinte
- une limite est atteinte
- une plage de blocage commence ou se termine

### FR11. Synchronisation

Le systeme doit synchroniser les regles et les preferences entre le backend et le client iOS avec un mode resilient aux pertes reseau.

### FR12. Journal d'evenements

Le systeme doit stocker les changements de regles, autorisations et evenements importants pour debugging et support.

## Exigences non fonctionnelles

### NFR1. Clarte produit

Toute promesse utilisateur doit distinguer:

- ce que fait la PWA
- ce que fait l'app iOS native
- ce qui depend des autorisations Apple

### NFR2. Performance

- chargement initial PWA inferieur a 3 secondes sur reseau normal
- dashboard interactif inferieur a 1 seconde apres chargement des donnees

### NFR3. Securite

- authentification securisee
- chiffrement TLS partout
- protection des tokens de session
- minimisation des donnees collectees

### NFR4. Confidentialite

- journalisation minimale
- conservation configurable
- aucune exposition inutile de donnees d'usage sensibles

### NFR5. Fiabilite

- les regles actives doivent etre reconcilees apres redemarrage ou reprise reseau
- les erreurs de synchro doivent etre visibles et recuperables

### NFR6. Accessibilite

- parcours essentiels conformes WCAG AA cote web
- lisibilite elevee pour ecrans de blocage

## Contraintes techniques

- Frontend web: Angular avec capacites PWA
- Backend: Spring Boot
- iPhone uniquement au MVP
- couche native iOS obligatoire pour enforcement

## Parcours utilisateur critiques

### Parcours 1. Premiere activation

1. L'utilisateur decouvre le produit.
2. Il installe la PWA ou ouvre l'interface web.
3. Il cree son compte.
4. Il telecharge ou relie l'app iOS native.
5. Il accorde les autorisations Apple necessaires.
6. Il configure sa premiere limite.
7. Il voit la regle active.

### Parcours 2. Limite quotidienne

1. L'utilisateur choisit Instagram.
2. Il fixe 30 minutes par jour.
3. Le systeme applique la regle.
4. A l'approche du seuil, il recoit un avertissement.
5. A depassement, le shield s'affiche.

### Parcours 3. Blocage web

1. L'utilisateur ajoute un ou plusieurs domaines.
2. La couche iOS applique le filtrage supporte.
3. Une tentative d'ouverture d'un domaine bloque affiche le shield.

## Open questions

- le produit est-il distribue comme une seule app iOS native avec webviews Angular, ou comme duo PWA + app iOS companion
- le niveau exact de friction pour override doit-il etre strict ou flexible
- faut-il un mode sans compte pour usage local uniquement

## Recommandation produit

Pour le MVP iPhone, la meilleure trajectoire est une app iOS principale avec une experience visuelle et logique de produit alignee sur Angular, completee par une PWA marketing/admin si besoin. Une PWA pure ne doit pas etre vendue comme mecanisme principal de blocage.

