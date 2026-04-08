import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

type Zone = 'METROPOLE' | 'ALSACE_LORRAINE';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss'
})
export class RegisterPageComponent {
  private static readonly registerUrl = '/api/v1/auth/register';

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  protected readonly submitting = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');
  protected readonly selectedPhotoName = signal('');

  protected readonly form = this.fb.nonNullable.group({
    pseudo: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    birthDate: ['', Validators.required],
    geographicZone: ['METROPOLE' as Zone, Validators.required],
    legalVersion: ['v1', Validators.required],
    consentAccepted: [false, Validators.requiredTrue]
  });

  private profilePhotoFile: File | null = null;

  protected onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    this.profilePhotoFile = file;
    this.selectedPhotoName.set(file?.name ?? '');
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const payload = new FormData();
    const value = this.form.getRawValue();
    payload.set('pseudo', value.pseudo);
    payload.set('email', value.email);
    payload.set('password', value.password);
    payload.set('birthDate', value.birthDate);
    payload.set('geographicZone', value.geographicZone);
    payload.set('legalVersion', value.legalVersion);
    payload.set('consentAccepted', String(value.consentAccepted));

    if (this.profilePhotoFile) {
      payload.set('profilePhoto', this.profilePhotoFile);
    }

    this.http.post<{ data: { email: string } }>(RegisterPageComponent.registerUrl, payload).subscribe({
      next: ({ data }) => {
        this.successMessage.set(`Compte créé pour ${data.email}. Vérifie ta boîte mail pour l'activation.`);
        this.form.reset({
          pseudo: '',
          email: '',
          password: '',
          birthDate: '',
          geographicZone: 'METROPOLE',
          legalVersion: 'v1',
          consentAccepted: false
        });
        this.profilePhotoFile = null;
        this.selectedPhotoName.set('');
        this.submitting.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error?.error?.error?.message ?? 'Impossible de créer le compte.');
        this.submitting.set(false);
      }
    });
  }
}
