# Hotfix: Suppression du bail de grâce streak

Status: review

## Story

En tant qu'utilisateur,
je veux que ma streak se casse réellement quand je n'ai pas eu de tâches pendant plusieurs jours,
afin que la streak reflète mon activité réelle et non un artefact algorithmique.

## Acceptance Criteria

1. Un jour sans aucune occurrence (vide) casse immédiatement la streak, quelle que soit la position dans le temps.
2. Le bail de grâce de 7 jours est supprimé.
3. Les jours transparents (skipped/canceled uniquement) restent neutres.
4. Les tests couvrent le cas inactivité 5+ jours avec occurrences absentes.

## Tasks / Subtasks

- [x] Supprimer le bail de grâce (lignes 87-93 de StreakService.java) et faire casser la streak sur jours vides (AC: 1, 2)
- [x] Mettre à jour les tests unitaires pour couvrir les nouveaux cas (AC: 3, 4)

## Dev Notes

- Fichier cible : `apps/api/src/main/java/com/ia/api/today/StreakService.java`
- Fichier tests : `apps/api/src/test/java/com/ia/api/today/StreakServiceTest.java`
- Le bail de grâce actuel (lignes 87-93) permet à des jours sans occurrences d'être ignorés dans la fenêtre de 7 jours — comportement à supprimer.
- Les jours transparents (ligne 100-103) doivent rester inchangés : un jour avec uniquement skipped/canceled reste neutre.

## Dev Agent Record

### Agent Model Used
Claude Sonnet 4.6

### Debug Log References

### Completion Notes List
- AC1/2 : Suppression du bail de grâce 7 jours (lignes 87-93). Un jour sans occurrence brise maintenant immédiatement la streak.
- AC3 : Les jours purement transparents (skipped/canceled) restent neutres — comportement inchangé (ligne 96-98).
- AC4 : 9 tests unitaires créés couvrant : jours vides, 5j inactivité, cas nominaux, transparent, suspension, missed, planned passé.

### File List
- apps/api/src/main/java/com/ia/api/today/StreakService.java (modified)
- apps/api/src/test/java/com/ia/api/today/StreakServiceTest.java (new)

### Change Log
- 2026-04-03 : Suppression du bail de grâce 7 jours — les jours sans occurrences cassent maintenant la streak directement.
