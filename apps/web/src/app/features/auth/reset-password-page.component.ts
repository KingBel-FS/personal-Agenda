import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password-page.component.html',
  styleUrl: './reset-password-page.component.scss'
})
export class ResetPasswordPageComponent {
  private static readonly confirmUrl = '/api/v1/auth/password-reset/confirm';
  private static readonly jsonHeaders = new HttpHeaders({
    'Content-Type': 'application/json',
    Accept: 'application/json'
  });

  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly submitting = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage = signal('');
  protected readonly resetToken = signal('');

  protected readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.resetToken.set(params.get('token') ?? '');
      if (!this.resetToken()) {
        this.errorMessage.set('Lien de réinitialisation invalide.');
      }
    });
  }

  protected submit(): void {
    if (!this.resetToken()) {
      this.errorMessage.set('Lien de réinitialisation invalide.');
      return;
    }
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.http
      .post<{ data: { message: string } }>(ResetPasswordPageComponent.confirmUrl, {
        token: this.resetToken(),
        newPassword: this.form.getRawValue().newPassword
      }, { headers: ResetPasswordPageComponent.jsonHeaders })
      .subscribe({
        next: ({ data }) => {
          this.successMessage.set(data.message);
          this.submitting.set(false);
        },
        error: (error) => {
          this.errorMessage.set(
            error?.error?.error?.message ?? 'Impossible de réinitialiser le mot de passe.'
          );
          this.submitting.set(false);
        }
      });
  }
}
