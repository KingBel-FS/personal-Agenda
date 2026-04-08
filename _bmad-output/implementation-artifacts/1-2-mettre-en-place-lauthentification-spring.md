# Story 1.2: Mettre en place l'authentification Spring

Status: ready-for-dev

## Story

As a utilisateur,
I want creer un compte et me connecter,
so that mes regles soient sauvegardees.

## Acceptance Criteria

1. Etant donne un utilisateur non authentifie, quand il s'inscrit ou se connecte, alors le backend cree ou authentifie son compte.
2. Le frontend doit recuperer une session valide et afficher un etat connecte coherent.
3. Le backend Spring doit proteger les endpoints necessitant une authentification.
4. Les erreurs d'inscription et de connexion doivent etre compréhensibles cote UI.

## Tasks / Subtasks

- [ ] Definir le modele utilisateur et le stockage associe
- [ ] Exposer les endpoints d'inscription et connexion
- [ ] Integrer Spring Security
- [ ] Connecter Angular aux flux login/register
- [ ] Gerer l'etat de session et les erreurs UI
- [ ] Ajouter les tests backend et frontend essentiels

## Dev Notes

- Auth simple, securisee et evolutive.
- Prevoir la compatibilite avec liaison d'appareil iPhone plus tard.

### Project Structure Notes

- Backend: `auth-service`, `user`, `security`
- Frontend: `core/auth`, `features/onboarding/auth`

### References

- [prd-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\prd-focuslock-ios-2026-03-28.md)
- [architecture-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\architecture-focuslock-ios-2026-03-28.md)
- [epics-and-stories-focuslock-ios-2026-03-28.md](C:\Dev\blocker\_bmad-output\planning-artifacts\epics-and-stories-focuslock-ios-2026-03-28.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story derivee du backlog BMAD

### Completion Notes List

- Prete pour implementation

### File List

- C:\Dev\blocker\_bmad-output\implementation-artifacts\1-2-mettre-en-place-lauthentification-spring.md

