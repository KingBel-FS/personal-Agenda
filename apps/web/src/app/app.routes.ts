import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { guestGuard } from './core/guest.guard';
import { AppShellComponent } from './core/app-shell.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'today' },

  { path: 'login', loadComponent: () => import('./features/auth/login-page.component').then((m) => m.LoginPageComponent), canActivate: [guestGuard] },
  { path: 'forgot-password', loadComponent: () => import('./features/auth/forgot-password-page.component').then((m) => m.ForgotPasswordPageComponent), canActivate: [guestGuard] },
  { path: 'register', loadComponent: () => import('./features/auth/register-page.component').then((m) => m.RegisterPageComponent), canActivate: [guestGuard] },

  { path: 'activate', loadComponent: () => import('./features/auth/activate-page.component').then((m) => m.ActivatePageComponent) },
  { path: 'reset-password', loadComponent: () => import('./features/auth/reset-password-page.component').then((m) => m.ResetPasswordPageComponent) },
  { path: 'legal', loadComponent: () => import('./features/legal/legal-page.component').then((m) => m.LegalPageComponent) },

  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'today', loadComponent: () => import('./features/today/today-page.component').then((m) => m.TodayPageComponent) },
      { path: 'agenda', loadComponent: () => import('./features/agenda/agenda-page.component').then((m) => m.AgendaPageComponent) },
      { path: 'goals', loadComponent: () => import('./features/goals/goals-page.component').then((m) => m.GoalsPageComponent) },
      { path: 'stats', loadComponent: () => import('./features/stats/stats-page.component').then((m) => m.StatsPageComponent) },
      { path: 'exports', loadComponent: () => import('./features/exports/exports-page.component').then((m) => m.ExportsPageComponent) },
      { path: 'daily', loadComponent: () => import('./features/today/daily-view-page.component').then((m) => m.DailyViewPageComponent) },
      { path: 'profile', loadComponent: () => import('./features/profile/profile-page.component').then((m) => m.ProfilePageComponent) },
      { path: 'tasks/new', loadComponent: () => import('./features/tasks/task-create-page.component').then((m) => m.TaskCreatePageComponent) },
      { path: 'tasks/manage', loadComponent: () => import('./features/tasks/task-manage-page.component').then((m) => m.TaskManagePageComponent) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notification-center-page.component').then((m) => m.NotificationCenterPageComponent) },
      { path: 'focuslock', loadComponent: () => import('./features/focuslock/focuslock-dashboard-page.component').then((m) => m.FocuslockDashboardPageComponent) },
      { path: 'focuslock/rules', loadComponent: () => import('./features/focuslock/focuslock-rules-page.component').then((m) => m.FocuslockRulesPageComponent) },
      { path: 'focuslock/insights', loadComponent: () => import('./features/focuslock/focuslock-insights-page.component').then((m) => m.FocuslockInsightsPageComponent) },
      { path: 'focuslock/device', loadComponent: () => import('./features/focuslock/focuslock-device-page.component').then((m) => m.FocuslockDevicePageComponent) }
    ]
  }
];
