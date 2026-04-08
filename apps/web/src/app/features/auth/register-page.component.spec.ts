import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { RegisterPageComponent } from './register-page.component';

describe('RegisterPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('sends a multipart registration payload with optional profile photo', () => {
    const fixture = TestBed.createComponent(RegisterPageComponent);
    const component = fixture.componentInstance as any;
    fixture.detectChanges();

    component.form.setValue({
      pseudo: 'alice',
      email: 'alice@example.com',
      password: 'Password123',
      birthDate: '1995-05-12',
      geographicZone: 'METROPOLE',
      legalVersion: 'v1',
      consentAccepted: true
    });

    const file = new File(['image'], 'avatar.png', { type: 'image/png' });
    component.onPhotoSelected({
      target: {
        files: {
          item: () => file
        }
      }
    } as unknown as Event);

    component.submit();

    const req = httpTesting.expectOne('/api/v1/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect(req.request.body.get('pseudo')).toBe('alice');
    expect(req.request.body.get('profilePhoto')).toBe(file);

    req.flush({ data: { email: 'alice@example.com' } });
    expect(component.successMessage()).toContain('alice@example.com');
  });
});
