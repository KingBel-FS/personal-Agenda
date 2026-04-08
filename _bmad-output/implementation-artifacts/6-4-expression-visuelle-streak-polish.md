# Story 6.4: Expression visuelle de la streak et polish final de confiance

Status: review

## Story

As a user,  
I want motivation cues that are clear but not intrusive,  
so that I feel progress without losing trust in the product.

## Acceptance Criteria

1. La streak active utilise une flamme vive et la streak cassée une flamme éteinte.
2. Les célébrations de badge sont brèves, mémorables et dismissibles.
3. Les feedbacks restent factuels et non punitifs.
4. Le polish préserve accessibilité et budgets de performance.

## Tasks / Subtasks

- [x] Implémenter le composant `StreakFlame` et ses variantes visuelles (AC: 1)
- [x] Implémenter les célébrations de badge avec fallback reduced-motion (AC: 2, 4)
- [x] Harmoniser les microcopies de feedback critique (AC: 3)
- [x] Vérifier coût perf et accessibilité du polish final (AC: 4)
- [x] Tester rendu dans Today et Stats avec différents états de streak (AC: 1, 2, 3)

## Dev Notes

- La récompense doit être sobre et méritée, jamais infantilisante. [Source: [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)]

### References

- [epics.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/epics.md)
- [ux-design-specification.md](c:/Dev/personal-Agenda/_bmad-output/planning-artifacts/ux-design-specification.md)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `npm test -- --watch=false --browsers=ChromeHeadless`
- `npm run build`
- `docker compose up --build -d web`

### Completion Notes List

- Ajout d'un composant `StreakFlame` avec états visuels distincts pour série active et série cassée.
- Ajout d'une célébration de badge sobre, dismissible et compatible `prefers-reduced-motion` au niveau du shell.
- Harmonisation des microcopies de feedback de Today pour rester factuelles et non punitives.
- Intégration de la streak dans Stats afin de vérifier le rendu cross-page et le polish final.

### File List

- apps/web/src/app/core/streak.service.ts
- apps/web/src/app/shared/components/streak-flame.component.ts
- apps/web/src/app/core/app-shell.component.ts
- apps/web/src/app/core/app-shell.component.html
- apps/web/src/app/core/app-shell.component.scss
- apps/web/src/app/features/today/today-page.component.ts
- apps/web/src/app/features/today/today-page.component.html
- apps/web/src/app/features/today/today-page.component.scss
- apps/web/src/app/features/stats/stats-page.component.ts
- apps/web/src/app/features/stats/stats-page.component.html
- apps/web/src/app/features/stats/stats-page.component.scss
- apps/web/src/styles.scss
