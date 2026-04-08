import { Component, inject, OnInit, signal } from '@angular/core';
import { NotificationApiService, type NotificationItem } from './notification-api.service';

@Component({
  selector: 'app-notification-center-page',
  templateUrl: './notification-center-page.component.html',
  styleUrl: './notification-center-page.component.scss'
})
export class NotificationCenterPageComponent implements OnInit {
  private readonly api = inject(NotificationApiService);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly notifications = signal<NotificationItem[]>([]);
  protected readonly totalCount = signal(0);
  protected readonly hasMore = signal(false);
  protected readonly currentPage = signal(0);

  ngOnInit(): void {
    this.load(0);
  }

  protected loadMore(): void {
    this.load(this.currentPage() + 1);
  }

  protected markViewed(item: NotificationItem): void {
    if (item.status === 'VIEWED' || item.status === 'DISMISSED') return;
    this.api.markViewed(item.id).subscribe({
      next: () => {
        this.notifications.update(list =>
          list.map(n => n.id === item.id ? { ...n, status: 'VIEWED' } : n)
        );
      }
    });
  }

  protected dismissNotification(item: NotificationItem): void {
    this.api.dismiss(item.id).subscribe({
      next: () => {
        this.notifications.update(list => list.filter(n => n.id !== item.id));
        this.totalCount.update(c => c - 1);
      }
    });
  }

  protected openNotification(item: NotificationItem): void {
    this.markViewed(item);
  }

  protected onNotificationKeydown(event: KeyboardEvent, item: NotificationItem): void {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }
    event.preventDefault();
    this.markViewed(item);
  }

  protected notificationAriaLabel(item: NotificationItem): string {
    const status = item.status === 'RECEIVED' ? 'non lue' : 'consultée';
    return `${item.title}. ${this.typeLabel(item.notificationType)}. ${this.relativeTime(item.createdAt)}. Notification ${status}.`;
  }

  protected relativeTime(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return "À l'instant";
    if (mins < 60) return `Il y a ${mins} min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `Il y a ${hours}h`;
    const days = Math.floor(hours / 24);
    return `Il y a ${days}j`;
  }

  protected typeLabel(type: string): string {
    switch (type) {
      case 'SCHEDULED_TASK': return 'Tâche';
      case 'STREAK_DANGER':  return 'Streak';
      case 'ANNIVERSARY':    return 'Anniversaire';
      case 'WEEKLY_GOAL':    return 'Objectif';
      case 'MONTHLY_GOAL':   return 'Objectif';
      default:               return type;
    }
  }

  private load(page: number): void {
    this.loading.set(true);
    this.api.getCenter(page).subscribe({
      next: ({ data }) => {
        if (page === 0) {
          this.notifications.set(data.notifications);
        } else {
          this.notifications.update(list => [...list, ...data.notifications]);
        }
        this.totalCount.set(data.totalCount);
        this.hasMore.set(data.hasMore);
        this.currentPage.set(page);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error?.message ?? 'Erreur de chargement.');
        this.loading.set(false);
      }
    });
  }
}
