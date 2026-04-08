import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationCenterPageComponent } from './notification-center-page.component';
import { authInterceptor } from '../../core/auth.interceptor';
import { AuthService } from '../../core/auth.service';

describe('NotificationCenterPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationCenterPageComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    }).compileComponents();

    TestBed.inject(AuthService).setAccessToken('test-token');
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    TestBed.inject(AuthService).clearAccessToken();
  });

  it('renders notifications as keyboard-accessible buttons with explicit labels', () => {
    const fixture = TestBed.createComponent(NotificationCenterPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) =>
      request.url === '/api/v1/notifications/center'
      && request.params.get('page') === '0'
      && request.params.get('size') === '20'
    ).flush({
      data: {
        notifications: [
          {
            id: 'notif-1',
            title: 'Rappel',
            body: 'Teste ta routine',
            notificationType: 'SCHEDULED_TASK',
            status: 'RECEIVED',
            createdAt: '2026-03-27T10:00:00Z',
            iconUrl: null
          }
        ],
        totalCount: 1,
        hasMore: false
      }
    });

    fixture.detectChanges();

    const item: HTMLElement | null = fixture.nativeElement.querySelector('.notif-card');
    expect(item?.getAttribute('role')).toBe('button');
    expect(item?.getAttribute('tabindex')).toBe('0');
    expect(item?.getAttribute('aria-label')).toContain('Notification non lue');
  });
});
