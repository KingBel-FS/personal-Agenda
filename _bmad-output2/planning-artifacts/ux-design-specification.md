---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
inputDocuments:
  - C:\Dev\blocker\_bmad-output\planning-artifacts\product-brief-focuslock-ios-2026-03-28.md
  - C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md
---

# UX Design Specification FocusLock iPhone

**Author:** Gino Cachondeo
**Date:** 2026-03-28

---

## Experience cible

L'experience doit donner la sensation de verrouiller ses distractions de facon volontaire, adulte et rassurante. On ne parle pas de punition mais de pacte avec soi-meme.

## Principes UX

- clarifier les limites techniques des le debut
- reduire au minimum l'effort pour creer une premiere regle
- afficher l'etat du systeme en permanence
- rendre le contournement possible mais psychologiquement couteux
- transformer le suivi en feedback motivant plutot qu'en surveillance anxieuse

## Personas

### Solo disciplinarian

Utilisateur adulte qui veut se proteger de ses propres automatismes. Il cherche du controle, pas un outil parental.

### Focus worker

Utilisateur qui veut bloquer certaines apps et sites pendant les heures de travail ou d'etude.

## Architecture d'information

- Accueil
- Regles
- Sessions
- Insights
- Parametres
- Etat iPhone

## Ecrans cles

### 1. Landing d'activation

Objectif: expliquer en 30 secondes comment le produit fonctionne sur iPhone.

Contenu:

- promesse simple
- avertissement honnete sur la necessite des permissions Apple
- CTA principal: commencer

### 2. Onboarding

Etapes:

- creation compte
- verification appareil cible: iPhone
- explication des autorisations
- etat de connexion app iOS
- creation premiere regle

### 3. Dashboard

Widgets:

- temps utilise aujourd'hui
- regles les plus proches du seuil
- prochaine plage de blocage
- streak de jours respectes
- CTA rapide: ajouter une regle

### 4. Catalogue de restrictions

L'utilisateur choisit entre:

- application
- categorie
- domaine web
- routine horaire

### 5. Editeur de regle

Champs:

- nom de la regle
- cible
- type: limite quotidienne ou plage horaire
- valeur de la limite
- jours applicables
- comportement de depassement
- niveau de friction pour override

### 6. Shield screen

Doit afficher:

- message bref et ferme
- nom de la regle ayant declenche le blocage
- compteur ou prochaine liberation
- action secondaire facultative: demander une exception

### 7. Insights

Visualisations:

- top distractions
- progression hebdomadaire
- jours ou les limites ont tenu
- heures critiques de rechute

### 8. Parametres

- etat des permissions Apple
- statut de synchronisation
- notifications
- export ou suppression des donnees

## Parcours UX detailles

### Parcours A. Premiere regle en moins de 2 minutes

1. L'utilisateur voit une promesse claire.
2. Il comprend qu'un composant iPhone natif est necessaire.
3. Il se connecte.
4. Il autorise les capacites requises.
5. Il choisit Instagram.
6. Il definit 30 minutes par jour.
7. Il voit la regle active sur le dashboard.

### Parcours B. Blocage de sites distractifs

1. L'utilisateur ouvre Regles.
2. Il choisit Domaine web.
3. Il saisit ou selectionne un domaine.
4. Il attache le domaine a une limite ou une plage de blocage.
5. Il confirme.
6. Il voit le statut applique.

### Parcours C. Contournement conscient

1. Le shield s'affiche.
2. L'utilisateur peut quitter ou lancer une demande d'override.
3. Le systeme impose une friction.
4. L'action est journalisee.

## Composants UX

- carte de statut
- jauge de consommation
- timeline de blocage
- builder de regles
- badge d'etat appareil
- modal de friction
- ecran shield

## Etats et feedback

- en attente de permission
- actif
- presque atteint
- bloque
- sync en erreur
- extension iOS indisponible

## Direction visuelle

Style recommande: editorial-tech sobre.

- Typo: titres forts et compacts, texte simple et lisible
- Couleurs: fond ivoire froid, encre anthracite, accent rouge brique pour blocage, vert sauge pour progression
- Formes: cartes larges, coins francs, peu d'effets decoratifs
- Motion: transitions courtes et signifiantes sur activation et seuils

## Accessibilite et responsive

- toutes les actions critiques doivent etre realisables a une main sur mobile
- contrastes AA minimum
- textes essentiels sans dependance a la couleur
- support lecteur d'ecran pour etats de regles et boutons d'override

## Contenu et ton

- ton direct, calme, adulte
- pas de vocabulaire moralisateur
- labels preferes: limite, verrou, respiration, reprise de controle

## Risques UX

- confusion entre PWA et enforcement natif
- frustration si l'utilisateur pense que tout peut etre fait depuis Safari seul
- surcomplexite du parametrage si on expose trop d'options dans le MVP

## Decision UX finale

L'UX doit mettre au centre l'etat de l'iPhone et l'etat des autorisations. La premiere promesse produit ne doit jamais etre "une PWA qui bloque votre iPhone", mais "une experience FocusLock sur iPhone, pilotee par une interface moderne et soutenue par les capacites natives Apple".

