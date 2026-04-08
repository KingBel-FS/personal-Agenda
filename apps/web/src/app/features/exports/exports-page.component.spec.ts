import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ExportsPageComponent } from './exports-page.component';
import { authInterceptor } from '../../core/auth.interceptor';
import { AuthService } from '../../core/auth.service';

xdescribe('ExportsPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExportsPageComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    TestBed.inject(AuthService).setAccessToken('test-token');
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    TestBed.inject(AuthService).clearAccessToken();
  });

  it('loads export history on init', () => {
    const fixture = TestBed.createComponent(ExportsPageComponent);
    fixture.detectChanges();

    httpTesting.expectOne((request) => request.url === '/api/v1/exports/history').flush({
      data: {
        items: [
          {
            id: '1',
            exportFormat: 'CSV',
            exportScope: 'FULL',
            periodFrom: '2026-03-20',
            periodTo: '2026-03-27',
            status: 'SUCCESS',
            fileName: 'export.csv',
            rowCount: 4,
            durationMs: 25,
            createdAt: '2026-03-27T10:00:00Z',
            completedAt: '2026-03-27T10:00:01Z'
          }
        ]
      }
    });

    const component = fixture.componentInstance as any;
    expect(component.history().length).toBe(1);
  });
});
